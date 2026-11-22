-- Keep the existing time-zone preference as a single application setting.
create table app_settings (
    id integer primary key check (id = 1),
    time_zone text not null
);

insert into app_settings (id, time_zone)
select 1, time_zone from workspaces
where id = 'a1d86a7c-6c20-462e-8d2e-4fcee925a10a';

-- Dropping this column also removes its foreign key and dependent indexes.
alter table roles drop column workspace_id;
drop table workspaces;

create index roles_activity on roles (last_activity_at desc, id);
create index roles_stage on roles (stage, id);
create index roles_follow_up on roles (next_action_due_on, id)
    where stage <> 'COMPLETED' and next_action_due_on is not null;
create index roles_completion_suggestions on roles (stage_changed_at, id)
    where stage in ('APPLIED', 'IN_PROGRESS') and not completion_suggestion_dismissed;
