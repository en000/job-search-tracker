package com.jobtracker.services;

import com.jobtracker.domain.enums.TodayKind;

import java.time.*;
import java.util.*;
import com.jobtracker.domain.*;
import com.jobtracker.dtos.responses.*;
import com.jobtracker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import static com.jobtracker.domain.enums.TodayKind.*;

@Service
public class TrackerService {
    private final AppSettingsService settings;
    private final TrackerQueries queries;
    private final RoleRepository roles;
    private final InterviewService interviews;
    private final AssignmentService assignments;
    private final RoleEventService events;
    private final Clock clock;

    TrackerService(AppSettingsService settings, TrackerQueries queries, RoleRepository roles,
                     InterviewService interviews, AssignmentService assignments, RoleEventService events, Clock clock) {
        this.settings = settings;
        this.queries = queries;
        this.roles = roles;
        this.interviews = interviews;
        this.assignments = assignments;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Today today() {
        ZoneId zone = ZoneId.of(settings.settings().timeZone());
        Instant now = Inputs.now(clock);
        LocalDate date = LocalDate.ofInstant(now, zone);
        List<DatedItem> overdue = new ArrayList<>();
        List<DatedItem> dueToday = new ArrayList<>();
        List<DatedItem> upcoming = new ArrayList<>();
        List<TodayItem> unscheduled = new ArrayList<>();
        for (TrackerQueries.WorkRow row : queries.today()) {
            var kind = com.jobtracker.domain.enums.TodayKind.valueOf(row.getKind());
            ZonedDateTime start = row.getStartsAt() == null ? null : row.getStartsAt().atZone(zone);
            LocalDate due = start == null ? row.getDueOn() : start.toLocalDate();
            if (kind == SCHEDULED_INTERVIEW && due.isBefore(date)) kind = RECORD_INTERVIEW_OUTCOME;
            TodayItem item = new TodayItem(kind, row.getRoleId(), row.getCompany(), row.getRoleTitle(),
                    row.getLabel(), row.getChildId(), row.getDueOn(), row.getStartsAt(), row.getRoleVersion());
            if (due == null) {
                unscheduled.add(item);
                continue;
            }
            DatedItem dated = new DatedItem(item, due, start == null ? LocalTime.MIDNIGHT : start.toLocalTime());
            if (due.isBefore(date)) overdue.add(dated);
            else if (due.equals(date)) dueToday.add(dated);
            else upcoming.add(dated);
        }

        unscheduled.sort(Comparator.comparing(TodayItem::company, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(TodayItem::roleTitle, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(item -> item.kind().name()).thenComparing(TrackerService::stableId));
        List<CompletionSuggestion> suggestions = queries
                .completionSuggestions(now.minus(Role.COMPLETION_SUGGESTION_DELAY), now).stream()
                .map(row -> new CompletionSuggestion(row.getRoleId(), row.getCompany(), row.getRoleTitle(), row.getRoleVersion()))
                .toList();
        return new Today(date, zone.getId(), sorted(overdue), sorted(dueToday), sorted(upcoming), unscheduled, suggestions);
    }

    private record DatedItem(TodayItem item, LocalDate date, LocalTime time) {}

    private static List<TodayItem> sorted(List<DatedItem> items) {
        return items.stream().sorted(Comparator.comparing(DatedItem::date).thenComparing(DatedItem::time)
                .thenComparing(dated -> dated.item.kind().name()).thenComparing(dated -> stableId(dated.item)))
                .map(DatedItem::item).toList();
    }

    private static String stableId(TodayItem item) {
        return (item.childId() == null ? item.roleId() : item.childId()).toString();
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Export export() {
        Instant now = Inputs.now(clock);
        Settings snapshotSettings = settings.settings();
        List<ExportRole> exported = roles.findAllByOrderByLastActivityAtDescIdAsc()
                .stream().map(role -> new ExportRole(ViewMapper.role(role), interviews.listInterviews(role.id),
                        assignments.listAssignments(role.id), events.list(role.id))).toList();
        return new Export(3, now, snapshotSettings, exported);
    }
}
