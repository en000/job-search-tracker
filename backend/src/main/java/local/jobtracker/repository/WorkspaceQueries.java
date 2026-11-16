package local.jobtracker.repository;

import java.time.*;
import java.util.*;
import local.jobtracker.domain.Role;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface WorkspaceQueries extends Repository<Role, UUID> {
    interface RoleRow {
        UUID getId();
        String getCompany();
        String getTitle();
        String getStage();
        String getSubstage();
        String getOutcome();
        Instant getLastActivityAt();
        String getItemKind();
        UUID getChildId();
        String getItemLabel();
        LocalDate getDueOn();
        Instant getStartsAt();
    }

    interface WorkRow {
        UUID getRoleId();
        UUID getChildId();
        String getCompany();
        String getRoleTitle();
        String getKind();
        String getLabel();
        LocalDate getDueOn();
        Instant getStartsAt();
        Long getRoleVersion();
    }

    String ROLE_FILTER = """
        where r.workspace_id = :workspace
          and (:pattern = '' or lower(r.company) like lower(:pattern) escape '!'
                             or lower(r.title) like lower(:pattern) escape '!')
          and (:stage = '' or r.stage = :stage)
          and (:substage = '' or r.substage = :substage)
          and (:outcome = '' or r.outcome = :outcome)
        """;

    @Query(value = """
        select r.id, r.company, r.title, r.stage, r.substage, r.outcome, r.last_activity_at as "lastActivityAt",
               n.kind as "itemKind", n.child_id as "childId", n.label as "itemLabel", n.due_on as "dueOn",
               n.starts_at as "startsAt"
        from roles r
        left join lateral (
            select work.*
            from (
                select 'FOLLOW_UP' as kind, cast(null as uuid) as child_id, r.next_action_text as label,
                       r.next_action_due_on as due_on, cast(null as timestamptz) as starts_at
                where r.next_action_due_on is not null
                union all
                select 'ASSIGNMENT', a.id, a.title, a.due_on, null
                from assignments a
                where a.role_id = r.id and a.status in ('PENDING', 'IN_PROGRESS') and a.due_on is not null
                union all
                select 'SCHEDULED_INTERVIEW', i.id, i.title, null, i.starts_at
                from interviews i
                where i.role_id = r.id and i.status = 'SCHEDULED' and i.starts_at >= :now
            ) work
            cross join lateral (
                select coalesce(work.starts_at, cast(work.due_on as timestamp) at time zone :zone) as instant
            ) date_key
            order by date_key.instant, work.kind, work.child_id
            limit 1
        ) n on r.stage <> 'COMPLETED'
        """ + ROLE_FILTER + """
        order by case when :nextFirst then coalesce(n.starts_at, cast(n.due_on as timestamp) at time zone :zone) end nulls last,
                 case when not :nextFirst then r.last_activity_at end desc, r.id
        limit :limit offset :offset
        """, nativeQuery = true)
    List<RoleRow> roles(UUID workspace, String pattern, String stage, String substage, String outcome,
                        Instant now, String zone, boolean nextFirst, int limit, long offset);

    @Query(value = "select count(*) from roles r " + ROLE_FILTER, nativeQuery = true)
    long roleCount(UUID workspace, String pattern, String stage, String substage, String outcome);

    @Query(value = """
        select r.id as "roleId", r.company, r.title as "roleTitle", w.kind, w.label, w.child_id as "childId",
               w.due_on as "dueOn", w.starts_at as "startsAt", w.role_version as "roleVersion"
        from roles r
        cross join lateral (
            select 'FOLLOW_UP' as kind, r.next_action_text as label, cast(null as uuid) as child_id,
                   r.next_action_due_on as due_on, cast(null as timestamptz) as starts_at, r.version as role_version
            where r.next_action_text is not null
            union all
            select 'ASSIGNMENT', a.title, a.id, a.due_on, null, null
            from assignments a where a.role_id = r.id and a.status in ('PENDING', 'IN_PROGRESS')
            union all
            select case when i.status = 'PLANNED' then 'PLANNED_INTERVIEW' else 'SCHEDULED_INTERVIEW' end,
                   i.title, i.id, null, case when i.status = 'SCHEDULED' then i.starts_at end, null
            from interviews i where i.role_id = r.id and i.status in ('SCHEDULED', 'PLANNED')
        ) w
        where r.workspace_id = :workspace and r.stage <> 'COMPLETED'
        """, nativeQuery = true)
    List<WorkRow> today(UUID workspace);

    interface SuggestionRow {
        UUID getRoleId();
        String getCompany();
        String getRoleTitle();
        long getRoleVersion();
    }

    @Query("""
        select r.id as roleId, r.company as company, r.title as roleTitle, r.version as roleVersion
        from Role r
        where r.workspaceId = :workspace and r.stage in (APPLIED, IN_PROGRESS)
          and r.stageChangedAt <= :cutoff and not r.completionSuggestionDismissed
          and (r.completionSuggestionSnoozedUntil is null or r.completionSuggestionSnoozedUntil <= :now)
        order by r.stageChangedAt, r.id
        """)
    List<SuggestionRow> completionSuggestions(UUID workspace, Instant cutoff, Instant now);
}
