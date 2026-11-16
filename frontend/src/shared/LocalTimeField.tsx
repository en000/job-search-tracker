import { useId } from 'react';
import { Field } from './ui';
import { resolveLocalTime, toLocalInput } from './zonedTime';

export type TimeDraft = { local: string; offset: string };
export const blankTime: TimeDraft = { local: '', offset: '' };

export function initialTime(instant: string | null, zone: string): TimeDraft {
  return { local: toLocalInput(instant, zone), offset: '' };
}

function resolve(draft: TimeDraft, zone: string, original: string | null) {
  if (original && draft.local === toLocalInput(original, zone) && !draft.offset) {
    return { kind: 'valid', instant: original } as const;
  }
  return resolveLocalTime(draft.local, zone);
}

export function instantValue(draft: TimeDraft, zone: string, original: string | null) {
  const value = resolve(draft, zone, original);
  switch (value.kind) {
    case 'empty': return null;
    case 'valid': return value.instant;
    case 'ambiguous': return value.choices.find((choice) => choice.offset === draft.offset)?.instant;
    case 'invalid': return undefined;
  }
}

export function LocalTimeField({ name, label, draft, setDraft, zone, original, error, required }: {
  name: string; label: string; draft: TimeDraft; setDraft: (value: TimeDraft) => void;
  zone: string; original: string | null; error?: unknown; required?: boolean;
}) {
  const resolution = resolve(draft, zone, original);
  const offsetId = useId();
  return <div className="time-field">
    <Field name={name} label={label} type="datetime-local" step={1} value={draft.local}
      onChange={(event) => setDraft({ local: event.target.value, offset: '' })} required={required}
      hint={zone} error={error} message={resolution.kind === 'invalid' ? resolution.message : undefined} />
    {resolution.kind === 'ambiguous' && <div className="field">
      <label htmlFor={offsetId}>{label} offset</label>
      <select id={offsetId} aria-describedby={offsetId + '-hint'} required value={draft.offset}
        onChange={(event) => setDraft({ ...draft, offset: event.target.value })}>
        <option value="">Choose an offset</option>
        {resolution.choices.map((choice) => <option key={choice.offset} value={choice.offset}>UTC{choice.offset}</option>)}
      </select>
      <small id={offsetId + '-hint'}>This time occurs twice in {zone}.</small>
    </div>}
  </div>;
}
