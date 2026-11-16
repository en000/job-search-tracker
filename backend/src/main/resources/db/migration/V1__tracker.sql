create table workspaces (
    id uuid primary key,
    time_zone text not null,
    created_at timestamptz not null
);

insert into workspaces values ('a1d86a7c-6c20-462e-8d2e-4fcee925a10a', 'UTC', current_timestamp);

create table roles (
    id uuid primary key,
    workspace_id uuid not null references workspaces(id),
    company text not null check (company <> '' and company = btrim(company)),
    title text not null check (title <> '' and title = btrim(title)),
    posting_url text,
    location text,
    work_arrangement text,
    salary_text text,
    notes text,
    stage text not null check (stage in ('SAVED', 'APPLIED', 'IN_PROGRESS', 'COMPLETED')),
    substage text,
    previous_stage text check (previous_stage in ('SAVED', 'APPLIED', 'IN_PROGRESS')),
    previous_substage text,
    outcome text check (outcome in ('ACCEPTED', 'REJECTED', 'WITHDRAWN', 'NO_RESPONSE')),
    completed_at timestamptz,
    next_action_text text check (next_action_text <> '' and next_action_text = btrim(next_action_text)),
    next_action_due_on date,
    version bigint not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    last_activity_at timestamptz not null,
    check (substage is null or coalesce((stage = 'APPLIED' and substage in ('APPLICATION_SUBMITTED', 'AWAITING_RESPONSE')) or
        (stage = 'IN_PROGRESS' and substage in ('INTERVIEW_SCHEDULED', 'FIRST_INTERVIEW_PASSED', 'AWAITING_INTERVIEW_FEEDBACK', 'ASSIGNMENT_PENDING', 'ASSIGNMENT_SUBMITTED', 'FINAL_INTERVIEW', 'OFFER_RECEIVED')), false)),
    check (previous_substage is null or coalesce((previous_stage = 'APPLIED' and previous_substage in ('APPLICATION_SUBMITTED', 'AWAITING_RESPONSE')) or
        (previous_stage = 'IN_PROGRESS' and previous_substage in ('INTERVIEW_SCHEDULED', 'FIRST_INTERVIEW_PASSED', 'AWAITING_INTERVIEW_FEEDBACK', 'ASSIGNMENT_PENDING', 'ASSIGNMENT_SUBMITTED', 'FINAL_INTERVIEW', 'OFFER_RECEIVED')), false)),
    check ((stage = 'COMPLETED' and previous_stage is not null and outcome is not null and completed_at is not null and next_action_text is null)
        or (stage <> 'COMPLETED' and previous_stage is null and previous_substage is null and outcome is null and completed_at is null)),
    check (next_action_due_on is null or next_action_text is not null)
);

create table interviews (
    id uuid primary key,
    role_id uuid not null references roles(id),
    title text not null check (title <> '' and title = btrim(title)),
    type text not null check (type in ('HR_RECRUITER', 'TECHNICAL', 'HIRING_MANAGER', 'PANEL', 'FINAL', 'OTHER')),
    status text not null check (status in ('PLANNED', 'SCHEDULED', 'AWAITING_FEEDBACK', 'PASSED', 'DID_NOT_PASS', 'CANCELLED')),
    round_number int check (round_number > 0),
    starts_at timestamptz,
    duration_minutes int check (duration_minutes > 0),
    meeting_url text,
    location text,
    prep_notes text,
    interview_notes text,
    feedback text,
    version bigint not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    unique (role_id, id),
    check (status <> 'SCHEDULED' or starts_at is not null)
);

create table interview_participants (
    id uuid primary key,
    interview_id uuid not null references interviews(id),
    name text not null check (name <> '' and name = btrim(name)),
    position int not null check (position >= 0),
    job_title text,
    email text,
    unique (interview_id, position) deferrable initially deferred
);

create table assignments (
    id uuid primary key,
    role_id uuid not null references roles(id),
    title text not null check (title <> '' and title = btrim(title)),
    status text not null check (status in ('PENDING', 'IN_PROGRESS', 'SUBMITTED', 'PASSED', 'DID_NOT_PASS', 'CANCELLED')),
    received_on date,
    due_on date,
    brief text,
    brief_url text,
    work_url text,
    submission_url text,
    work_notes text,
    feedback text,
    submitted_at timestamptz,
    version bigint not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    unique (role_id, id),
    check (status not in ('SUBMITTED', 'PASSED', 'DID_NOT_PASS') or submitted_at is not null)
);

create table role_events (
    id uuid primary key,
    role_id uuid not null references roles(id),
    interview_id uuid,
    assignment_id uuid,
    kind text not null check (kind in ('ROLE_CREATED', 'ROLE_STAGE_CHANGED', 'ROLE_COMPLETED', 'ROLE_REOPENED', 'FOLLOW_UP_COMPLETED', 'INTERVIEW_CREATED', 'INTERVIEW_STATUS_CHANGED', 'ASSIGNMENT_CREATED', 'ASSIGNMENT_STATUS_CHANGED', 'NOTE', 'RECRUITER_CONVERSATION')),
    occurred_at timestamptz not null,
    created_at timestamptz not null,
    body text,
    details jsonb not null default '{}'::jsonb,
    foreign key (role_id, interview_id) references interviews(role_id, id),
    foreign key (role_id, assignment_id) references assignments(role_id, id),
    check (interview_id is null or assignment_id is null),
    check (jsonb_typeof(details) = 'object')
);

create index roles_activity on roles (workspace_id, last_activity_at desc, id);
create index roles_stage on roles (workspace_id, stage, id);
create index roles_follow_up on roles (workspace_id, next_action_due_on, id)
    where stage <> 'COMPLETED' and next_action_due_on is not null;
create index interviews_scheduled on interviews (role_id, starts_at, id) where status = 'SCHEDULED';
create index assignments_open on assignments (role_id, due_on, id)
    where status in ('PENDING', 'IN_PROGRESS') and due_on is not null;
create index role_events_timeline on role_events (role_id, occurred_at desc, created_at desc, id);
create index role_events_interview on role_events (role_id, interview_id) where interview_id is not null;
create index role_events_assignment on role_events (role_id, assignment_id) where assignment_id is not null;
