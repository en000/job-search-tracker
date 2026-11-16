import { useId, type InputHTMLAttributes, type ReactNode } from 'react';
import { ApiError, fieldError } from './api';
import { label } from './labels';

type FieldProps = { label: string; name: string; error?: unknown; message?: string; hint?: string };

export function Field({ label, name, error, message, hint, ...input }: FieldProps & InputHTMLAttributes<HTMLInputElement>) {
  const id = useId();
  const issue = message ?? fieldError(error, name);
  return <div className="field">
    <label htmlFor={id}>{label}</label>
    <input {...input} id={id} name={name} aria-invalid={!!issue} aria-describedby={issue || hint ? id + '-help' : undefined} />
    {issue || hint ? <small id={id + '-help'} className={issue ? 'issue' : ''}>{issue ?? hint}</small> : null}
  </div>;
}

export function TextArea({ label, name, error, hint, defaultValue, required }: FieldProps &
  { defaultValue?: string | null; required?: boolean }) {
  const id = useId();
  const issue = fieldError(error, name);
  return <div className="field">
    <label htmlFor={id}>{label}</label>
    <textarea id={id} name={name} defaultValue={defaultValue ?? ''} maxLength={20000} required={required}
      aria-invalid={!!issue} aria-describedby={issue || hint ? id + '-help' : undefined} rows={4} />
    {issue || hint ? <small id={id + '-help'} className={issue ? 'issue' : ''}>{issue ?? hint}</small> : null}
  </div>;
}

export function SelectField<T extends string>({ name, label: caption, options, empty, value, defaultValue, onChange, error }: FieldProps &
  { options: readonly T[]; empty?: string; value?: string; defaultValue?: string; onChange?: (value: T | '') => void }) {
  const id = useId();
  const issue = fieldError(error, name);
  return <div className="field">
    <label htmlFor={id}>{caption}</label>
    <select id={id} name={name} aria-invalid={!!issue} aria-describedby={issue ? id + '-help' : undefined}
      value={value} defaultValue={defaultValue} onChange={onChange ? (event) => onChange(event.target.value as T | '') : undefined}>
      {empty !== undefined && <option value="">{empty}</option>}
      {options.map((option) => <option key={option} value={option}>{label(option)}</option>)}
    </select>
    {issue && <small id={id + '-help'} className="issue">{issue}</small>}
  </div>;
}

export function Pill({ code }: { code: string }) {
  return <span className={'pill pill-' + code.toLowerCase().replaceAll('_', '-')}>{label(code)}</span>;
}

export function LoadState({ error, retry }: { error: unknown; retry: () => unknown }) {
  if (error) return <div className="surface empty" role="alert">
    <p>{error instanceof Error ? error.message : 'The page could not be loaded.'}</p>
    <button className="button secondary" onClick={retry}>Try again</button>
  </div>;
  return <p className="surface empty" role="status">Loading…</p>;
}

export function WriteError({ error }: { error: unknown }) {
  if (!error) return null;
  const issues = error instanceof ApiError ? Object.entries(error.problem.fieldErrors ?? {}) : [];
  return <div className="write-error" role="alert">
    <p>{error instanceof Error ? error.message : 'The changes could not be saved.'}</p>
    {issues.length > 0 && <ul>{issues.map(([field, message]) =>
      <li key={field}><strong>{field.replace(/([a-z])([A-Z])/g, '$1 $2')}</strong> — {message}</li>)}</ul>}
  </div>;
}

export function VersionReview({ error, current, expected, onApply, children }: {
  error: unknown; current: number; expected: number; onApply: () => void; children: ReactNode;
}) {
  if (!(error instanceof ApiError) || !error.stale) return null;
  return <section className="review" aria-label="Latest saved version">
    <h3>Latest saved version</h3>
    {current === expected ? <p>Waiting for the latest version. Refresh the page if this persists.</p> : <>
      {children}
      <button type="button" className="button secondary" onClick={onApply}>Keep my edits and use this version</button>
    </>}
  </section>;
}

export function PageHeading({ title, caption, children }: { title: string; caption?: string; children?: ReactNode }) {
  return <div className="page-heading">
    <div>{caption && <div className="page-caption">{caption}</div>}<h1>{title}</h1></div>
    {children && <div className="heading-actions">{children}</div>}
  </div>;
}

export function Pagination({ page, size, total, onPage }: {
  page: number; size: number; total: number; onPage: (page: number) => void;
}) {
  const pages = Math.max(1, Math.ceil(total / size));
  return pages > 1 || page > 0 ? <nav className="pagination" aria-label="Pagination">
    <button className="button secondary small" disabled={page <= 0} onClick={() => onPage(page - 1)}>Previous</button>
    <span>Page {page + 1} of {pages}</span>
    <button className="button secondary small" disabled={page + 1 >= pages} onClick={() => onPage(page + 1)}>Next</button>
  </nav> : null;
}

export function formText(form: FormData, field: string) {
  return String(form.get(field) ?? '').trim();
}

export function nullableText(form: FormData, field: string) {
  return formText(form, field) || null;
}
