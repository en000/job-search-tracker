import { useQuery } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { queries, send, useWrite } from '../../shared/api';
import type { Settings } from '../../shared/types';
import { Field, LoadState, PageHeading, WriteError } from '../../shared/ui';

const browserZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
const zoneOptions = [...new Set(['UTC', browserZone, ...Intl.supportedValuesOf('timeZone')])].sort();

function ZoneForm({ settings }: { settings: Settings }) {
  const [zone, setZone] = useState(settings.timeZone);
  const write = useWrite((timeZone: string) => send<Settings>('/settings', 'PUT', { timeZone }));

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!write.isPending) write.mutate(zone.trim(), { onSuccess: (saved) => setZone(saved.timeZone) });
  }

  return <form className="surface panel form-stack" onSubmit={submit}>
    <div><h2>Time zone</h2><p className="muted">Today and interview times use this zone. Calendar due dates stay the same.</p></div>
    <WriteError error={write.error} />
    <Field label="Tracker time zone" name="timeZone" list="time-zones" placeholder="Europe/Warsaw" maxLength={100}
      value={zone} onChange={(event) => { setZone(event.target.value); write.reset(); }} required error={write.error} />
    <datalist id="time-zones">{zoneOptions.map((option) => <option key={option} value={option} />)}</datalist>
    <div className="form-actions">
      <button type="submit" className="button" disabled={write.isPending}>{write.isPending ? 'Saving…' : 'Save time zone'}</button>
      {browserZone && zone !== browserZone && <button type="button" className="button secondary"
        onClick={() => { setZone(browserZone); write.reset(); }}>Use browser time zone ({browserZone})</button>}
    </div>
    {write.isSuccess && <p className="saved-message" role="status">Time zone saved.</p>}
  </form>;
}

export function SettingsPage() {
  const result = useQuery(queries.settings());
  return <>
    <PageHeading title="Settings" />
    <div className="editor form-stack">
      {result.data ? <ZoneForm settings={result.data} /> : <LoadState error={result.error} retry={result.refetch} />}
      <section className="surface panel form-stack">
        <div><h2>Workspace export</h2><p className="muted">Download all roles, interviews, assignments, follow-ups, and timeline history as JSON.</p></div>
        <div><a className="button secondary" href="/api/export" download="job-search-tracker.json">Download JSON</a></div>
      </section>
    </div>
  </>;
}
