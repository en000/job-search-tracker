import { useQuery } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { queries, send, useWrite } from '../../shared/api';
import type { Settings, Theme } from '../../shared/types';
import { Field, LoadState, PageHeading, SelectField, WriteError } from '../../shared/ui';

const browserZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
const zoneOptions = [...new Set(['UTC', browserZone, ...Intl.supportedValuesOf('timeZone')])].sort();

function PreferencesForm({ settings }: { settings: Settings }) {
  const [zone, setZone] = useState(settings.timeZone);
  const [theme, setTheme] = useState<Theme>(settings.theme);
  const write = useWrite((input: Settings) => send<Settings>('/settings', 'PUT', input));

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!write.isPending) write.mutate({ timeZone: zone.trim(), theme }, { onSuccess: (saved) => { setZone(saved.timeZone); setTheme(saved.theme); } });
  }

  return <form className="surface panel form-stack" onSubmit={submit}>
    <div><h2>Appearance and time zone</h2><p className="muted">Today and interview times use this zone. Calendar due dates stay the same.</p></div>
    <WriteError error={write.error} />
    <SelectField label="Theme" name="theme" options={['LIGHT', 'DARK'] as const} value={theme}
      onChange={(value) => { if (value) { setTheme(value); write.reset(); } }} error={write.error} />
    <Field label="Tracker time zone" name="timeZone" list="time-zones" placeholder="Europe/Warsaw" maxLength={100}
      value={zone} onChange={(event) => { setZone(event.target.value); write.reset(); }} required error={write.error} />
    <datalist id="time-zones">{zoneOptions.map((option) => <option key={option} value={option} />)}</datalist>
    <div className="form-actions">
      <button type="submit" className="button" disabled={write.isPending}>{write.isPending ? 'Saving…' : 'Save settings'}</button>
      {browserZone && zone !== browserZone && <button type="button" className="button secondary"
        onClick={() => { setZone(browserZone); write.reset(); }}>Use browser time zone ({browserZone})</button>}
    </div>
    {write.isSuccess && <p className="saved-message" role="status">Settings saved.</p>}
  </form>;
}

export function SettingsPage() {
  const result = useQuery(queries.settings());
  return <>
    <PageHeading title="Settings" />
    <div className="editor form-stack">
      {result.data ? <PreferencesForm settings={result.data} /> : <LoadState error={result.error} retry={result.refetch} />}
      <section className="surface panel form-stack">
        <div><h2>Export data</h2><p className="muted">Download all roles, interviews, assignments, follow-ups, and timeline history as JSON.</p></div>
        <div><a className="button secondary" href="/api/export" download="job-search-tracker.json">Download JSON</a></div>
      </section>
    </div>
  </>;
}
