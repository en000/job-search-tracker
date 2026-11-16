alter table roles
    add column stage_changed_at timestamptz,
    add column completion_suggestion_dismissed boolean not null default false,
    add column completion_suggestion_snoozed_until timestamptz;

update roles r
set stage_changed_at = coalesce(
    (select max(e.created_at) from role_events e
     where e.role_id = r.id and e.kind in ('ROLE_CREATED', 'ROLE_STAGE_CHANGED', 'ROLE_COMPLETED', 'ROLE_REOPENED')),
    r.created_at);

update roles
set substage = case when stage = 'APPLIED' then null else substage end,
    previous_substage = case when previous_stage = 'APPLIED' then null else previous_substage end,
    version = version + 1
where (stage = 'APPLIED' and substage is not null)
   or (previous_stage = 'APPLIED' and previous_substage is not null);

alter table roles
    alter column stage_changed_at set not null,
    add constraint roles_substage_only_in_progress check (substage is null or stage = 'IN_PROGRESS'),
    add constraint roles_previous_substage_only_in_progress
        check (previous_substage is null or previous_stage is not distinct from 'IN_PROGRESS');

create index roles_completion_suggestions on roles (workspace_id, stage_changed_at, id)
    where stage in ('APPLIED', 'IN_PROGRESS') and not completion_suggestion_dismissed;
