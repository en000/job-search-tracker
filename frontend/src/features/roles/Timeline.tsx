import { useQuery } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { queries, send, useWrite } from '../../shared/api';
import { formatDate, formatInstant } from '../../shared/dates';
import { label } from '../../shared/labels';
import { blankTime, instantValue, LocalTimeField } from '../../shared/LocalTimeField';
import type { ReturnState } from '../../shared/navigation';
import type { TimelineEvent } from '../../shared/types';
import { formText, LoadState, Pagination, SelectField, TextArea, WriteError } from '../../shared/ui';

function describe(value: unknown): string {
  if (typeof value === 'string') return label(value);
  if (value && typeof value === 'object') {
    const state = value as Record<string, unknown>;
    return [state.status, state.stage, state.outcome].filter((part): part is string => typeof part === 'string')
      .map(label).join(' / ');
  }
  return '';
}

export function Timeline({ roleId, zone, state }: { roleId: string; zone: string; state: ReturnState }) {
  const [page, setPage] = useState(0);
  const [logging, setLogging] = useState(false);
  const [draft, setDraft] = useState(blankTime);
  const result = useQuery(queries.timeline(roleId, page));
  const rolePath = '/roles/' + roleId;
  const write = useWrite((payload: object) => send<TimelineEvent>(rolePath + '/timeline', 'POST', payload));

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const occurredAt = instantValue(draft, zone, null);
    if (occurredAt === undefined || write.isPending) return;
    const form = new FormData(event.currentTarget);
    write.mutate({ kind: formText(form, 'kind'), body: formText(form, 'body'), occurredAt }, { onSuccess: () => {
      setDraft(blankTime);
      setLogging(false);
      setPage(0);
    } });
  }

  return <section className="surface panel">
    <div className="section-heading"><h2>Timeline</h2>
      {!logging && <button className="button secondary small" onClick={() => setLogging(true)}>Log activity</button>}</div>
    {logging && <form className="form-stack inset-form" onSubmit={submit}>
      <WriteError error={write.error} />
      <SelectField label="Activity type" name="kind" options={['NOTE', 'RECRUITER_CONVERSATION']} error={write.error} />
      <TextArea label="Activity notes" name="body" required error={write.error} />
      <LocalTimeField name="occurredAt" label="When it happened (optional)" draft={draft} setDraft={setDraft} zone={zone}
        original={null} error={write.error} />
      <div className="form-actions">
        <button type="submit" className="button small" disabled={write.isPending}>Save activity</button>
        <button type="button" className="button secondary small" onClick={() => {
          setDraft(blankTime); setLogging(false); write.reset();
        }}>Cancel</button>
      </div>
    </form>}
    {!result.data ? <LoadState error={result.error} retry={result.refetch} /> : <>
      {result.data.items.length > 0 ? <ol className="timeline">
        {result.data.items.map((event) => {
          const before = describe(event.details.before);
          const after = describe(event.details.after);
          const child = event.interviewId ? rolePath + '/interviews/' + event.interviewId :
            event.assignmentId ? rolePath + '/assignments/' + event.assignmentId : null;
          return <li key={event.id}>
            <div className="timeline-event">
              <strong>{label(event.kind)}</strong>
              <time className="muted" dateTime={event.occurredAt}>{formatInstant(event.occurredAt, zone)}</time>
            </div>
            {child && <Link to={child} state={state}>{String(event.details.title ?? 'View record')}</Link>}
            {after && <p>{before ? before + ' → ' : ''}{after}</p>}
            {typeof event.details.text === 'string' && <p className="preserve">{event.details.text}
              {typeof event.details.dueOn === 'string' && ' · Due ' + formatDate(event.details.dueOn)}</p>}
            {event.body && <p className="preserve">{event.body}</p>}
          </li>;
        })}
      </ol> : <p className="muted">No activity yet.</p>}
      <Pagination page={result.data.page} size={result.data.size} total={result.data.total} onPage={setPage} />
    </>}
  </section>;
}
