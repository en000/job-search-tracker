package com.jobtracker.services;

import com.jobtracker.dtos.requests.Completion;
import com.jobtracker.dtos.requests.CompletionSuggestionChange;
import com.jobtracker.dtos.requests.FollowUp;
import com.jobtracker.dtos.requests.FollowUpChange;
import com.jobtracker.dtos.requests.RoleCreate;
import com.jobtracker.dtos.requests.RolePatch;
import com.jobtracker.dtos.requests.RoleSort;
import com.jobtracker.dtos.requests.Version;

import com.jobtracker.domain.enums.RoleStatus;
import com.jobtracker.domain.enums.Stage;
import com.jobtracker.domain.enums.Outcome;
import com.jobtracker.domain.enums.EventKind;
import com.jobtracker.domain.enums.TodayKind;

import java.time.*;
import java.util.*;
import com.jobtracker.domain.*;
import com.jobtracker.dtos.responses.*;
import com.jobtracker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import static com.jobtracker.services.Inputs.*;
import static com.jobtracker.services.TrackerException.*;

@Service
public class RoleService {
    private final AppSettingsService settings;
    private final RoleWorkflow workflow;
    private final RoleRepository roles;
    private final RoleEventService events;
    private final TrackerQueries queries;
    private final Clock clock;

    RoleService(AppSettingsService settings, RoleWorkflow workflow, RoleRepository roles,
                RoleEventService events, TrackerQueries queries, Clock clock) {
        this.settings = settings;
        this.workflow = workflow;
        this.roles = roles;
        this.events = events;
        this.queries = queries;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<RoleSummary> list(String q, RoleStatus status, Stage stage, Outcome outcome,
                                  RoleSort sort, int page, int size) {
        String pattern = q == null || q.isBlank() ? "" :
                "%" + q.strip().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        String zone = settings.settings().timeZone();
        String statusCode = code(status), stageCode = code(stage), outcomeCode = code(outcome);
        List<RoleSummary> items = queries.roles(pattern, statusCode, stageCode, outcomeCode,
                clock.instant(), zone, sort == RoleSort.nextScheduled, size, (long) page * size)
                .stream().map(RoleService::summary).toList();
        return new Page<>(items, page, size, queries.roleCount(pattern, statusCode, stageCode, outcomeCode));
    }

    private static RoleSummary summary(TrackerQueries.RoleRow row) {
        NextItem next = row.getItemKind() == null ? null : new NextItem(TodayKind.valueOf(row.getItemKind()),
                row.getChildId(), row.getItemLabel(), row.getDueOn(), row.getStartsAt());
        return new RoleSummary(row.getId(), row.getCompany(), row.getTitle(), RoleStatus.valueOf(row.getStatus()),
                row.getStage() == null ? null : Stage.valueOf(row.getStage()),
                row.getOutcome() == null ? null : Outcome.valueOf(row.getOutcome()), row.getLastActivityAt(), next);
    }

    private static String code(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    @Transactional(readOnly = true)
    public RoleDetails get(UUID id) {
        return ViewMapper.role(workflow.read(id));
    }

    @Transactional
    public RoleDetails create(RoleCreate request) {
        RoleStatus status = request.status() == null ? RoleStatus.SAVED : request.status();
        if (status != RoleStatus.SAVED && status != RoleStatus.APPLIED) {
            throw invalid("status", "Start a role in Saved or Applied.");
        }
        checkPair(status, request.stage());
        Instant now = now(clock);
        Role role = new Role();
        role.id = UUID.randomUUID();
        role.company = required(request.company(), "company");
        role.title = required(request.title(), "title");
        role.postingUrl = link(request.postingUrl(), "postingUrl");
        role.location = optional(request.location());
        role.workArrangement = optional(request.workArrangement());
        role.salaryText = optional(request.salaryText());
        role.notes = optional(request.notes());
        role.status = status;
        role.stage = request.stage();
        role.createdAt = now;
        role.updatedAt = now;
        role.recordStageChange(now);
        RoleWorkflow.touch(role, now);
        roles.saveAndFlush(role);
        events.event(role, EventKind.ROLE_CREATED, now, Map.of("after", RoleWorkflow.snapshot(role)));
        return ViewMapper.role(role);
    }

    @Transactional
    public RoleDetails patch(UUID id, RolePatch request, Set<String> fields) {
        Role role = lockedVersion(id, request.expectedVersion());
        RoleDetails before = ViewMapper.role(role);
        Map<String, Object> beforeStage = RoleWorkflow.snapshot(role);
        if (fields.contains("company")) role.company = required(request.company(), "company");
        if (fields.contains("title")) role.title = required(request.title(), "title");
        if (fields.contains("postingUrl")) role.postingUrl = link(request.postingUrl(), "postingUrl");
        if (fields.contains("location")) role.location = optional(request.location());
        if (fields.contains("workArrangement")) role.workArrangement = optional(request.workArrangement());
        if (fields.contains("salaryText")) role.salaryText = optional(request.salaryText());
        if (fields.contains("notes")) role.notes = optional(request.notes());

        if (fields.contains("status") || fields.contains("stage")) {
            setStage(role, request, fields);
        }
        if (before.equals(ViewMapper.role(role))) return before;
        Instant now = now(clock);
        if (before.status() != role.status || before.stage() != role.stage) {
            transition(role, EventKind.ROLE_STAGE_CHANGED, now, beforeStage);
        }
        return saveChange(role, now);
    }

    private static void setStage(Role role, RolePatch request, Set<String> fields) {
        RoleStatus status = fields.contains("status") ? request.status() : role.status;
        if (status == null) throw invalid("status", "Choose a status.");
        RoleWorkflow.requireActive(role);
        if (status == RoleStatus.COMPLETED) throw invalid("status", "Use Complete role and select an outcome.");
        Stage stage = fields.contains("stage") ? request.stage() : role.stage;
        if (!Role.isValidPair(status, stage)) {
            if (fields.contains("stage")) checkPair(status, stage);
            stage = null;
        }
        role.status = status;
        role.stage = stage;
    }

    private static void checkPair(RoleStatus status, Stage stage) {
        if (!Role.isValidPair(status, stage)) throw invalid("stage", "Choose a stage for this status.");
    }

    @Transactional
    public RoleDetails complete(UUID id, Completion request) {
        Role role = lockedVersion(id, request.expectedVersion());
        RoleWorkflow.requireActive(role);
        Instant now = now(clock);
        Map<String, Object> before = RoleWorkflow.snapshot(role);
        role.complete(request.outcome(), now);
        transition(role, EventKind.ROLE_COMPLETED, now, before);
        return saveChange(role, now);
    }

    @Transactional
    public RoleDetails reopen(UUID id, Version request) {
        Role role = lockedVersion(id, request.expectedVersion());
        if (role.isActive()) throw conflict("role_active", "This role is already active.");
        Instant now = now(clock);
        Map<String, Object> before = RoleWorkflow.snapshot(role);
        role.reopen();
        transition(role, EventKind.ROLE_REOPENED, now, before);
        return saveChange(role, now);
    }

    @Transactional
    public RoleDetails changeCompletionSuggestion(UUID id, CompletionSuggestionChange request) {
        Role role = lockedVersion(id, request.expectedVersion());
        Instant now = now(clock);
        if (!role.canSuggestCompletion(now)) {
            throw conflict("suggestion_unavailable", "This completion suggestion is no longer available.");
        }
        switch (request.action()) {
            case DISMISS -> {
                role.completionSuggestionDismissed = true;
                role.completionSuggestionSnoozedUntil = null;
            }
            case SNOOZE -> role.completionSuggestionSnoozedUntil = now.plus(Role.COMPLETION_SUGGESTION_DELAY);
        }
        roles.flush();
        return ViewMapper.role(role);
    }

    @Transactional
    public RoleDetails setFollowUp(UUID id, FollowUpChange request) {
        Role role = lockedVersion(id, request.expectedVersion());
        RoleWorkflow.requireActive(role);
        FollowUp followUp = request.followUp();
        String text = followUp == null ? null : required(followUp.text(), "followUp.text");
        LocalDate dueOn = followUp == null ? null : followUp.dueOn();
        if (Objects.equals(text, role.nextActionText) && Objects.equals(dueOn, role.nextActionDueOn)) {
            return ViewMapper.role(role);
        }
        role.nextActionText = text;
        role.nextActionDueOn = dueOn;
        return saveChange(role, now(clock));
    }

    @Transactional
    public RoleDetails completeFollowUp(UUID id, Version request) {
        Role role = lockedVersion(id, request.expectedVersion());
        RoleWorkflow.requireActive(role);
        if (role.nextActionText == null) throw conflict("no_follow_up", "This role has no current follow-up.");
        Instant now = now(clock);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("text", role.nextActionText);
        snapshot.put("dueOn", role.nextActionDueOn == null ? null : role.nextActionDueOn.toString());
        events.event(role, EventKind.FOLLOW_UP_COMPLETED, now, snapshot);
        role.clearFollowUp();
        return saveChange(role, now);
    }

    private Role lockedVersion(UUID id, Long expectedVersion) {
        Role role = workflow.lock(id);
        checkVersion(expectedVersion, role.version);
        return role;
    }

    private void transition(Role role, EventKind kind, Instant now, Map<String, Object> before) {
        role.recordStageChange(now);
        events.event(role, kind, now, Map.of("before", before, "after", RoleWorkflow.snapshot(role)));
    }

    private RoleDetails saveChange(Role role, Instant now) {
        role.updatedAt = now;
        RoleWorkflow.touch(role, now);
        roles.flush();
        return ViewMapper.role(role);
    }
}
