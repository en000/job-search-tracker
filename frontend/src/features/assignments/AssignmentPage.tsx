import { useQuery } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { queries, send, useWrite } from '../../shared/api';
import { formatDate, formatInstant } from '../../shared/dates';
import { label } from '../../shared/labels';
import { initialTime, instantValue, LocalTimeField } from '../../shared/LocalTimeField';
import { returnState } from '../../shared/navigation';
import type { Assignment, AssignmentFields } from '../../shared/types';
import { assignmentStatusCodes } from '../../shared/types';
import { Field, formText, LoadState, nullableText, PageHeading, SelectField, TextArea, VersionReview, WriteError } from '../../shared/ui';

function AssignmentForm({ assignment, roleId, timeZone }: { assignment?: Assignment; roleId: string; timeZone: string }) {
  const [zone] = useState(timeZone);
  const [original] = useState(assignment?.submittedAt ?? null);
  const [time, setTime] = useState(() => initialTime(original, zone));
  const [expected, setExpected] = useState(assignment?.version);
  const navigate = useNavigate();
  const state = returnState(useLocation().state);
  const rolePath = '/roles/' + roleId;
  const collection = rolePath + '/assignments';
  const write = useWrite((body: AssignmentFields & { expectedVersion?: number }) => assignment ?
    send<Assignment>(collection + '/' + assignment.id, 'PUT', body) : send<Assignment>(collection, 'POST', body));

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const submittedAt = instantValue(time, zone, original);
    if (submittedAt === undefined || write.isPending) return;
    const form = new FormData(event.currentTarget);
    write.mutate({
      expectedVersion: expected,
      title: formText(form, 'title'),
      status: formText(form, 'status') as AssignmentFields['status'],
      receivedOn: nullableText(form, 'receivedOn'),
      dueOn: nullableText(form, 'dueOn'),
      brief: nullableText(form, 'brief'),
      briefUrl: nullableText(form, 'briefUrl'),
      workUrl: nullableText(form, 'workUrl'),
      submissionUrl: nullableText(form, 'submissionUrl'),
      workNotes: nullableText(form, 'workNotes'),
      feedback: nullableText(form, 'feedback'),
      submittedAt: time.local === initialTime(original, zone).local && !time.offset ? null : submittedAt,
    }, { onSuccess: () => navigate(rolePath, { state }) });
  }

  const error = write.error;
  return <form className="editor form-stack" onSubmit={submit}>
    <WriteError error={error} />
    {assignment && expected !== undefined && <VersionReview error={error} current={assignment.version} expected={expected}
      onApply={() => { setExpected(assignment.version); write.reset(); }}>
      <dl className="facts">
        <div><dt>Title</dt><dd>{assignment.title}</dd></div>
        <div><dt>Status</dt><dd>{label(assignment.status)}</dd></div>
        <div><dt>Received</dt><dd>{assignment.receivedOn ? formatDate(assignment.receivedOn) : '—'}</dd></div>
        <div><dt>Due date</dt><dd>{assignment.dueOn ? formatDate(assignment.dueOn) : '—'}</dd></div>
        <div><dt>Instructions</dt><dd>{assignment.brief ?? '—'}</dd></div>
        <div><dt>Brief URL</dt><dd>{assignment.briefUrl ?? '—'}</dd></div>
        <div><dt>Work URL</dt><dd>{assignment.workUrl ?? '—'}</dd></div>
        <div><dt>Work notes</dt><dd>{assignment.workNotes ?? '—'}</dd></div>
        <div><dt>Submission URL</dt><dd>{assignment.submissionUrl ?? '—'}</dd></div>
        <div><dt>Submission time</dt><dd>{assignment.submittedAt ? formatInstant(assignment.submittedAt, zone) : '—'}</dd></div>
        <div><dt>Feedback</dt><dd>{assignment.feedback ?? '—'}</dd></div>
      </dl>
    </VersionReview>}
    <section className="surface panel form-stack">
      <Field label="Assignment title" name="title" required maxLength={200} defaultValue={assignment?.title} error={error} />
      <SelectField label="Status" name="status" options={assignmentStatusCodes} defaultValue={assignment?.status ?? 'PENDING'} error={error} />
      <div className="form-grid">
        <Field label="Received date" name="receivedOn" type="date" defaultValue={assignment?.receivedOn ?? ''} error={error} />
        <Field label="Due date" name="dueOn" type="date" required defaultValue={assignment?.dueOn ?? ''} error={error} />
      </div>
    </section>
    <section className="surface panel form-stack">
      <h2>Brief and work</h2>
      <TextArea label="Instructions" name="brief" defaultValue={assignment?.brief} error={error} />
      <div className="form-grid">
        <Field label="Brief URL" name="briefUrl" type="url" maxLength={2048} defaultValue={assignment?.briefUrl ?? ''} error={error} />
        <Field label="Work URL" name="workUrl" type="url" maxLength={2048} defaultValue={assignment?.workUrl ?? ''} error={error} />
      </div>
      <TextArea label="Work notes" name="workNotes" defaultValue={assignment?.workNotes} error={error} />
    </section>
    <section className="surface panel form-stack">
      <h2>Submission and feedback</h2>
      <div className="form-grid">
        <LocalTimeField label="Submission time" name="submittedAt" draft={time} setDraft={setTime} zone={zone} original={original}
          error={error} />
        <Field label="Submission URL" name="submissionUrl" type="url" maxLength={2048} defaultValue={assignment?.submissionUrl ?? ''} error={error} />
      </div>
      <TextArea label="Feedback" name="feedback" defaultValue={assignment?.feedback} error={error} />
    </section>
    <div className="form-actions">
      <button className="button" type="submit" disabled={write.isPending}>{write.isPending ? 'Saving…' : 'Save assignment'}</button>
      <Link className="button secondary" to={rolePath} state={state}>Cancel</Link>
    </div>
  </form>;
}

export function AssignmentPage() {
  const { roleId = '', assignmentId } = useParams();
  const role = useQuery(queries.role(roleId));
  const settings = useQuery(queries.settings());
  const record = useQuery({ ...queries.assignment(roleId, assignmentId ?? ''), enabled: !!assignmentId });
  const state = returnState(useLocation().state);

  return <>
    <Link to={'/roles/' + roleId} state={state} className="back-link">← Role details</Link>
    <PageHeading title={assignmentId ? 'Edit assignment' : 'Add assignment'} caption={role.data?.title} />
    {!role.data || !settings.data || (assignmentId && !record.data) ?
      <LoadState error={role.error ?? settings.error ?? record.error} retry={() => {
        void role.refetch(); void settings.refetch(); if (assignmentId) void record.refetch();
      }} /> : !assignmentId && role.data.stage === 'COMPLETED' ?
        <p className="surface empty">Reopen this role to add an assignment.</p> :
        <AssignmentForm key={roleId + (assignmentId ?? '')} roleId={roleId} assignment={record.data} timeZone={settings.data.timeZone} />}
  </>;
}
