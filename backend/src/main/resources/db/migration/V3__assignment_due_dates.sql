-- Require dates on new writes while preserving older assignments for review.
alter table assignments
    add constraint assignments_due_date_required check (due_on is not null) not valid;
