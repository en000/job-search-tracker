package local.jobtracker.services;

import java.time.*;
import java.util.*;
import local.jobtracker.domain.*;
import local.jobtracker.dtos.Responses.*;
import local.jobtracker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import static local.jobtracker.domain.Codes.TodayKind.*;

@Service
public class WorkspaceService {
    private final WorkspaceContext workspace;
    private final WorkspaceQueries queries;
    private final RoleRepository roles;
    private final ChildService children;
    private final EventRepository events;
    private final Clock clock;

    WorkspaceService(WorkspaceContext workspace, WorkspaceQueries queries, RoleRepository roles,
                     ChildService children, EventRepository events, Clock clock) {
        this.workspace = workspace;
        this.queries = queries;
        this.roles = roles;
        this.children = children;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Settings settings() {
        return new Settings(workspace.current().timeZone);
    }

    @Transactional
    public Settings updateZone(String zone) {
        if (!ZoneId.getAvailableZoneIds().contains(zone)) {
            throw TrackerException.invalid("timeZone", "Choose a valid IANA time zone.");
        }
        Workspace current = workspace.current();
        current.timeZone = zone;
        return new Settings(zone);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Today today() {
        ZoneId zone = ZoneId.of(workspace.current().timeZone);
        Instant now = Inputs.now(clock);
        LocalDate date = LocalDate.ofInstant(now, zone);
        List<DatedItem> overdue = new ArrayList<>();
        List<DatedItem> dueToday = new ArrayList<>();
        List<DatedItem> upcoming = new ArrayList<>();
        List<TodayItem> unscheduled = new ArrayList<>();
        for (WorkspaceQueries.WorkRow row : queries.today(workspace.id())) {
            var kind = local.jobtracker.domain.Codes.TodayKind.valueOf(row.getKind());
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
                .thenComparing(item -> item.kind().name()).thenComparing(WorkspaceService::stableId));
        List<CompletionSuggestion> suggestions = queries
                .completionSuggestions(workspace.id(), now.minus(Role.COMPLETION_SUGGESTION_DELAY), now).stream()
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
        Settings settings = new Settings(workspace.current().timeZone);
        List<ExportRole> exported = roles.findByWorkspaceIdOrderByLastActivityAtDescIdAsc(workspace.id())
                .stream().map(role -> new ExportRole(ViewMapper.role(role), children.listInterviews(role.id),
                        children.listAssignments(role.id), events.listScoped(workspace.id(), role.id)
                        .stream().map(ViewMapper::event).toList())).toList();
        return new Export(1, now, settings, exported);
    }
}
