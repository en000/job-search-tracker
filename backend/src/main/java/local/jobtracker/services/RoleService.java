package local.jobtracker.services;

import java.time.*;
import java.util.*;
import local.jobtracker.domain.*;
import local.jobtracker.dtos.Requests;
import local.jobtracker.dtos.Responses.*;
import local.jobtracker.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import static local.jobtracker.domain.Codes.*;
import static local.jobtracker.services.Inputs.*;
import static local.jobtracker.services.TrackerException.*;

@Service
public class RoleService {
    private final WorkspaceContext workspace;
    private final RoleWorkflow workflow;
    private final RoleRepository roles;
    private final EventRepository events;
    private final WorkspaceQueries queries;
    private final Clock clock;

    RoleService(WorkspaceContext workspace, RoleWorkflow workflow, RoleRepository roles,
                EventRepository events, WorkspaceQueries queries, Clock clock) {
        this.workspace = workspace;
        this.workflow = workflow;
        this.roles = roles;
        this.events = events;
        this.queries = queries;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<RoleSummary> list(String q, Stage stage, Substage substage, Outcome outcome,
                                  Requests.RoleSort sort, int page, int size) {
        String pattern = q == null || q.isBlank() ? "" :
                "%" + q.strip().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        String zone = workspace.current().timeZone;
        String stageCode = code(stage), substageCode = code(substage), outcomeCode = code(outcome);
        List<RoleSummary> items = queries.roles(workspace.id(), pattern, stageCode, substageCode, outcomeCode,
                clock.instant(), zone, sort == Requests.RoleSort.nextScheduled, size, (long) page * size)
                .stream().map(RoleService::summary).toList();
        return new Page<>(items, page, size, queries.roleCount(workspace.id(), pattern, stageCode, substageCode, outcomeCode));
    }

    private static RoleSummary summary(WorkspaceQueries.RoleRow row) {
        NextItem next = row.getItemKind() == null ? null : new NextItem(TodayKind.valueOf(row.getItemKind()),
                row.getChildId(), row.getItemLabel(), row.getDueOn(), row.getStartsAt());
        return new RoleSummary(row.getId(), row.getCompany(), row.getTitle(), Stage.valueOf(row.getStage()),
                row.getSubstage() == null ? null : Substage.valueOf(row.getSubstage()),
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
    public RoleDetails create(Requests.RoleCreate request) {
        Stage stage = request.stage() == null ? Stage.SAVED : request.stage();
        if (stage != Stage.SAVED && stage != Stage.APPLIED) {
            throw invalid("stage", "Start a role in Saved or Applied.");
        }
        checkPair(stage, request.substage());
        Instant now = now(clock);
        Role role = new Role();
        role.id = UUID.randomUUID();
        role.workspaceId = workspace.id();
        role.company = required(request.company(), "company");
        role.title = required(request.title(), "title");
        role.postingUrl = link(request.postingUrl(), "postingUrl");
        role.location = optional(request.location());
        role.workArrangement = optional(request.workArrangement());
        role.salaryText = optional(request.salaryText());
        role.notes = optional(request.notes());
        role.stage = stage;
        role.substage = request.substage();
        role.createdAt = now;
        role.updatedAt = now;
        role.recordStageChange(now);
        RoleWorkflow.touch(role, now);
        roles.saveAndFlush(role);
        workflow.event(role, EventKind.ROLE_CREATED, now, Map.of("after", RoleWorkflow.snapshot(role)));
        return ViewMapper.role(role);
    }

    @Transactional
    public RoleDetails patch(UUID id, Requests.RolePatch request, Set<String> fields) {
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

        if (fields.contains("stage") || fields.contains("substage")) {
            setStage(role, request, fields);
        }
        if (before.equals(ViewMapper.role(role))) return before;
        Instant now = now(clock);
        if (before.stage() != role.stage || before.substage() != role.substage) {
            transition(role, EventKind.ROLE_STAGE_CHANGED, now, beforeStage);
        }
        return saveChange(role, now);
    }

    private static void setStage(Role role, Requests.RolePatch request, Set<String> fields) {
        Stage stage = fields.contains("stage") ? request.stage() : role.stage;
        if (stage == null) throw invalid("stage", "Choose a stage.");
        RoleWorkflow.requireActive(role);
        if (stage == Stage.COMPLETED) throw invalid("stage", "Use Complete role and select an outcome.");
        Substage substage = fields.contains("substage") ? request.substage() : role.substage;
        if (!Role.isValidPair(stage, substage)) {
            if (fields.contains("substage")) checkPair(stage, substage);
            substage = null;
        }
        role.stage = stage;
        role.substage = substage;
    }

    private static void checkPair(Stage stage, Substage substage) {
        if (!Role.isValidPair(stage, substage)) throw invalid("substage", "Choose a substage for this stage.");
    }

    @Transactional
    public RoleDetails complete(UUID id, Requests.Completion request) {
        Role role = lockedVersion(id, request.expectedVersion());
        RoleWorkflow.requireActive(role);
        Instant now = now(clock);
        Map<String, Object> before = RoleWorkflow.snapshot(role);
        role.complete(request.outcome(), now);
        transition(role, EventKind.ROLE_COMPLETED, now, before);
        return saveChange(role, now);
    }

    @Transactional
    public RoleDetails reopen(UUID id, Requests.Version request) {
        Role role = lockedVersion(id, request.expectedVersion());
        if (role.isActive()) throw conflict("role_active", "This role is already active.");
        Instant now = now(clock);
        Map<String, Object> before = RoleWorkflow.snapshot(role);
        role.reopen();
        transition(role, EventKind.ROLE_REOPENED, now, before);
        return saveChange(role, now);
    }

    @Transactional
    public RoleDetails changeCompletionSuggestion(UUID id, Requests.CompletionSuggestionChange request) {
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
    public RoleDetails setFollowUp(UUID id, Requests.FollowUpChange request) {
        Role role = lockedVersion(id, request.expectedVersion());
        RoleWorkflow.requireActive(role);
        Requests.FollowUp followUp = request.followUp();
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
    public RoleDetails completeFollowUp(UUID id, Requests.Version request) {
        Role role = lockedVersion(id, request.expectedVersion());
        RoleWorkflow.requireActive(role);
        if (role.nextActionText == null) throw conflict("no_follow_up", "This role has no current follow-up.");
        Instant now = now(clock);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("text", role.nextActionText);
        snapshot.put("dueOn", role.nextActionDueOn == null ? null : role.nextActionDueOn.toString());
        workflow.event(role, EventKind.FOLLOW_UP_COMPLETED, now, snapshot);
        role.clearFollowUp();
        return saveChange(role, now);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<EventDetails> timeline(UUID id, int page, int size) {
        workflow.read(id);
        var result = events.pageScoped(workspace.id(), id, PageRequest.of(page, size));
        return new Page<>(result.map(ViewMapper::event).getContent(), page, size, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public EventDetails timelineEvent(UUID roleId, UUID eventId) {
        return events.findScoped(workspace.id(), roleId, eventId).map(ViewMapper::event)
                .orElseThrow(() -> missing("Timeline event"));
    }

    @Transactional
    public EventDetails log(UUID id, Requests.EventInput request) {
        Role role = workflow.lock(id);
        if (request.kind() != EventKind.NOTE && request.kind() != EventKind.RECRUITER_CONVERSATION) {
            throw invalid("kind", "Choose Note or Recruiter conversation.");
        }
        Instant now = now(clock);
        Instant occurredAt = request.occurredAt() == null ? now : timestamp(request.occurredAt());
        RoleEvent event = workflow.manualEvent(role, request.kind(), now, occurredAt, required(request.body(), "body"));
        RoleWorkflow.touch(role, now);
        return ViewMapper.event(event);
    }

    private Role lockedVersion(UUID id, Long expectedVersion) {
        Role role = workflow.lock(id);
        checkVersion(expectedVersion, role.version);
        return role;
    }

    private void transition(Role role, EventKind kind, Instant now, Map<String, Object> before) {
        role.recordStageChange(now);
        workflow.event(role, kind, now, Map.of("before", before, "after", RoleWorkflow.snapshot(role)));
    }

    private RoleDetails saveChange(Role role, Instant now) {
        role.updatedAt = now;
        RoleWorkflow.touch(role, now);
        roles.flush();
        return ViewMapper.role(role);
    }
}
