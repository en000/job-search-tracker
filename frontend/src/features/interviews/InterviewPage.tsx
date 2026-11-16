import { useQuery } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { queries, send, useWrite } from '../../shared/api';
import { formatInstant } from '../../shared/dates';
import { label } from '../../shared/labels';
import { initialTime, instantValue, LocalTimeField } from '../../shared/LocalTimeField';
import { returnState } from '../../shared/navigation';
import type { Interview, InterviewFields, InterviewStatus, Participant, Substage } from '../../shared/types';
import { interviewStatusCodes, interviewTypeCodes } from '../../shared/types';
import { Field, formText, LoadState, nullableText, PageHeading, SelectField, TextArea, VersionReview, WriteError } from '../../shared/ui';

type PersonRow = Participant & { key: string };

function suggestedSubstage(interview: Interview): Substage | null | undefined {
  switch (interview.status) {
    case 'PLANNED': return null;
    case 'SCHEDULED': return interview.type === 'FINAL' ? 'FINAL_INTERVIEW' : 'INTERVIEW_SCHEDULED';
    case 'AWAITING_FEEDBACK': return 'AWAITING_INTERVIEW_FEEDBACK';
    case 'PASSED': return interview.roundNumber === 1 && interview.type !== 'FINAL' ? 'FIRST_INTERVIEW_PASSED' : null;
    default: return undefined;
  }
}

export function InterviewForm({ interview, roleId, timeZone }: { interview?: Interview; roleId: string; timeZone: string }) {
  const [zone] = useState(timeZone);
  const [original] = useState(interview?.startsAt ?? null);
  const [time, setTime] = useState(() => initialTime(original, zone));
  const [status, setStatus] = useState<InterviewStatus>(interview?.status ?? 'PLANNED');
  const [expected, setExpected] = useState(interview?.version);
  const [people, setPeople] = useState<PersonRow[]>(() => (interview?.participants ?? [])
    .map((person, index) => ({ ...person, key: person.id ?? String(index) })));
  const navigate = useNavigate();
  const state = returnState(useLocation().state);
  const rolePath = '/roles/' + roleId;
  const collection = rolePath + '/interviews';
  const write = useWrite((body: InterviewFields & { expectedVersion?: number }) => interview ?
    send<Interview>(collection + '/' + interview.id, 'PUT', body) : send<Interview>(collection, 'POST', body));

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const startsAt = instantValue(time, zone, original);
    if (startsAt === undefined || write.isPending) return;
    const form = new FormData(event.currentTarget);
    const round = nullableText(form, 'roundNumber'), duration = nullableText(form, 'durationMinutes');
    write.mutate({
      expectedVersion: expected,
      title: formText(form, 'title'),
      type: formText(form, 'type') as InterviewFields['type'],
      status,
      roundNumber: round ? Number(round) : null,
      startsAt,
      durationMinutes: duration ? Number(duration) : null,
      meetingUrl: nullableText(form, 'meetingUrl'),
      location: nullableText(form, 'location'),
      prepNotes: nullableText(form, 'prepNotes'),
      interviewNotes: nullableText(form, 'interviewNotes'),
      feedback: nullableText(form, 'feedback'),
      participants: people.map((person, index) => ({
        id: person.id,
        name: formText(form, 'participants[' + index + '].name'),
        jobTitle: nullableText(form, 'participants[' + index + '].jobTitle'),
        email: nullableText(form, 'participants[' + index + '].email'),
      })),
    }, { onSuccess: (saved) => {
      const substage = suggestedSubstage(saved);
      const interviewStageSuggestion = substage !== undefined && (!interview || suggestedSubstage(interview) !== substage)
        ? { substage } : undefined;
      navigate(rolePath, { state: { ...state, interviewStageSuggestion } });
    } });
  }

  const error = write.error;
  return <form className="editor form-stack" onSubmit={submit}>
    <WriteError error={error} />
    {interview && expected !== undefined && <VersionReview error={error} current={interview.version} expected={expected}
      onApply={() => { setExpected(interview.version); write.reset(); }}>
      <dl className="facts">
        <div><dt>Title</dt><dd>{interview.title}</dd></div>
        <div><dt>Type and status</dt><dd>{label(interview.type)} · {label(interview.status)}</dd></div>
        <div><dt>Start time</dt><dd>{interview.startsAt ? formatInstant(interview.startsAt, zone) : '—'}</dd></div>
        <div><dt>Round and duration</dt><dd>{interview.roundNumber ?? '—'} · {interview.durationMinutes ?? '—'} minutes</dd></div>
        <div><dt>Location</dt><dd>{interview.location ?? '—'}</dd></div>
        <div><dt>Meeting URL</dt><dd>{interview.meetingUrl ?? '—'}</dd></div>
        <div><dt>Interviewers</dt><dd>{interview.participants.map((person) =>
          [person.name, person.jobTitle, person.email].filter(Boolean).join(' · ')).join('\n') || '—'}</dd></div>
        <div><dt>Preparation notes</dt><dd>{interview.prepNotes ?? '—'}</dd></div>
        <div><dt>Interview notes</dt><dd>{interview.interviewNotes ?? '—'}</dd></div>
        <div><dt>Feedback</dt><dd>{interview.feedback ?? '—'}</dd></div>
      </dl>
    </VersionReview>}
    <section className="surface panel form-stack">
      <Field label="Interview title" name="title" required maxLength={200} defaultValue={interview?.title} error={error} />
      <div className="form-grid">
        <SelectField label="Type" name="type" options={interviewTypeCodes} defaultValue={interview?.type ?? 'HR_RECRUITER'} error={error} />
        <SelectField label="Status" name="status" options={interviewStatusCodes} value={status}
          onChange={(value) => { if (value) setStatus(value); }} error={error} />
      </div>
    </section>
    <section className="surface panel form-stack">
      <h2>Schedule and meeting</h2>
      <LocalTimeField label="Start time" name="startsAt" draft={time} setDraft={setTime} zone={zone} original={original}
        error={error} required={status === 'SCHEDULED'} />
      <div className="form-grid">
        <Field label="Round number" name="roundNumber" type="number" min={1} max={2147483647} defaultValue={interview?.roundNumber ?? ''} error={error} />
        <Field label="Duration in minutes" name="durationMinutes" type="number" min={1} max={2147483647}
          defaultValue={interview?.durationMinutes ?? ''} error={error} />
        <Field label="Meeting URL" name="meetingUrl" type="url" maxLength={2048} defaultValue={interview?.meetingUrl ?? ''} error={error} />
        <Field label="Location" name="location" maxLength={200} defaultValue={interview?.location ?? ''} error={error} />
      </div>
    </section>
    <section className="surface panel form-stack">
      <div className="section-heading">
        <h2>Interviewers</h2>
        <button className="button secondary small" type="button" disabled={people.length >= 100} onClick={() =>
          setPeople([...people, { key: crypto.randomUUID(), name: '', jobTitle: null, email: null }])}>Add interviewer</button>
      </div>
      {people.length === 0 && <p className="muted">No interviewers added.</p>}
      {people.map((person, index) => {
        const name = 'participants[' + index + ']';
        return <fieldset className="person" key={person.key}>
          <legend>Interviewer {index + 1}</legend>
          <div className="form-grid">
            <Field label="Name" name={name + '.name'} required maxLength={200} defaultValue={person.name} error={error} />
            <Field label="Job title" name={name + '.jobTitle'} maxLength={200} defaultValue={person.jobTitle ?? ''} error={error} />
            <Field label="Email" name={name + '.email'} type="email" maxLength={254} defaultValue={person.email ?? ''} error={error} />
          </div>
          <button className="link-button" type="button" onClick={() => setPeople(people.filter((row) => row.key !== person.key))}>
            Remove interviewer {index + 1}
          </button>
        </fieldset>;
      })}
    </section>
    <section className="surface panel form-stack">
      <h2>Notes and feedback</h2>
      <TextArea label="Preparation notes" name="prepNotes" defaultValue={interview?.prepNotes} error={error} />
      <TextArea label="Interview notes" name="interviewNotes" defaultValue={interview?.interviewNotes} error={error} />
      <TextArea label="Feedback" name="feedback" defaultValue={interview?.feedback} error={error} />
    </section>
    <div className="form-actions">
      <button className="button" type="submit" disabled={write.isPending}>{write.isPending ? 'Saving…' : 'Save interview'}</button>
      <Link className="button secondary" to={rolePath} state={state}>Cancel</Link>
    </div>
  </form>;
}

export function InterviewPage() {
  const { roleId = '', interviewId } = useParams();
  const role = useQuery(queries.role(roleId));
  const settings = useQuery(queries.settings());
  const record = useQuery({ ...queries.interview(roleId, interviewId ?? ''), enabled: !!interviewId });
  const state = returnState(useLocation().state);

  return <>
    <Link to={'/roles/' + roleId} state={state} className="back-link">← Role details</Link>
    <PageHeading title={interviewId ? 'Edit interview' : 'Add interview'} caption={role.data?.title} />
    {!role.data || !settings.data || (interviewId && !record.data) ?
      <LoadState error={role.error ?? settings.error ?? record.error} retry={() => {
        void role.refetch(); void settings.refetch(); if (interviewId) void record.refetch();
      }} /> : !interviewId && role.data.stage === 'COMPLETED' ?
        <p className="surface empty">Reopen this role to add an interview.</p> :
        <InterviewForm key={roleId + (interviewId ?? '')} roleId={roleId} interview={record.data} timeZone={settings.data.timeZone} />}
  </>;
}
