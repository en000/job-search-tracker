alter table roles
    drop column previous_stage,
    drop column previous_substage;

alter table roles rename column stage to status;
alter table roles rename column substage to stage;
alter index roles_stage rename to roles_status;
alter table roles rename constraint roles_substage_only_in_progress to roles_stage_only_in_progress;

-- The old completion constraint depended on the removed previous-stage columns.
alter table roles add constraint roles_completion_state check (
    (status = 'COMPLETED' and outcome is not null and completed_at is not null and next_action_text is null)
    or (status <> 'COMPLETED' and outcome is null and completed_at is null)
);

-- Preserve readable before/after states in existing timeline entries.
update role_events
set details = jsonb_set(details, '{before}',
    ((details->'before') - 'stage' - 'substage') || jsonb_build_object(
        'status', details->'before'->'stage', 'stage', details->'before'->'substage'))
where jsonb_typeof(details->'before') = 'object' and (details->'before') ? 'substage';

update role_events
set details = jsonb_set(details, '{after}',
    ((details->'after') - 'stage' - 'substage') || jsonb_build_object(
        'status', details->'after'->'stage', 'stage', details->'after'->'substage'))
where jsonb_typeof(details->'after') = 'object' and (details->'after') ? 'substage';
