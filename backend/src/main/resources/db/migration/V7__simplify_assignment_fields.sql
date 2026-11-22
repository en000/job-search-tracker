-- Keep a distinct old submission link in the assignment's timeline before merging links.
insert into role_events (id, role_id, assignment_id, kind, occurred_at, created_at, body)
select gen_random_uuid(), role_id, id, 'NOTE', current_timestamp, current_timestamp,
       'Previous submission link: ' || submission_url
from assignments
where submission_url is not null and work_url is not null and submission_url <> work_url;

-- Older assignments may still lack a due date. Preserve that existing exception
-- while updating their links, then restore the rule for future writes.
alter table assignments drop constraint assignments_due_date_required;
update assignments set work_url = submission_url
where work_url is null and submission_url is not null;
alter table assignments
    add constraint assignments_due_date_required check (due_on is not null) not valid;

alter table assignments rename column brief to instructions;
alter table assignments rename column brief_url to instructions_url;
alter table assignments rename column work_notes to notes;
alter table assignments drop column submission_url;
