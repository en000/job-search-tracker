import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { send, useWrite } from '../../shared/api';
import type { Role, RoleFields } from '../../shared/types';
import { Field, formText, nullableText, PageHeading, SelectField, TextArea, VersionReview, WriteError } from '../../shared/ui';

const startStatuses = ['SAVED', 'APPLIED'] as const;

function readRole(form: FormData): RoleFields {
  return {
    company: formText(form, 'company'),
    title: formText(form, 'title'),
    postingUrl: nullableText(form, 'postingUrl'),
    location: nullableText(form, 'location'),
    workArrangement: nullableText(form, 'workArrangement'),
    salaryText: nullableText(form, 'salaryText'),
    notes: nullableText(form, 'notes'),
  };
}

export function RoleForm({ role, onClose }: { role?: Role; onClose?: () => void }) {
  const [expected, setExpected] = useState(role?.version);
  const navigate = useNavigate();
  const write = useWrite((body: object) => role ? send<Role>('/roles/' + role.id, 'PATCH', body) :
    send<Role>('/roles', 'POST', body));

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (write.isPending) return;
    const form = new FormData(event.currentTarget);
    const details = readRole(form);
    write.mutate(role ? { ...details, expectedVersion: expected } : {
      ...details, status: formText(form, 'status'),
    }, { onSuccess: (saved) => role ? onClose?.() : navigate('/roles/' + saved.id, { replace: true }) });
  }

  const error = write.error;
  return <form className={role ? 'form-stack' : 'surface form-stack editor'} onSubmit={submit}>
    <WriteError error={error} />
    {role && expected !== undefined && <VersionReview error={error} current={role.version} expected={expected}
      onApply={() => { setExpected(role.version); write.reset(); }}>
      <RoleFacts role={role} />
    </VersionReview>}
    <div className="form-grid">
      <Field label="Company" name="company" required maxLength={200} defaultValue={role?.company} error={error} />
      <Field label="Role title" name="title" required maxLength={200} defaultValue={role?.title} error={error} />
    </div>
    {!role && <div className="form-grid">
      <SelectField label="Starting status" name="status" options={startStatuses} defaultValue="SAVED" error={error} />
    </div>}
    <Field label="Posting URL" name="postingUrl" type="url" maxLength={2048} defaultValue={role?.postingUrl ?? ''} error={error} />
    <div className="form-grid">
      <Field label="Location" name="location" maxLength={200} defaultValue={role?.location ?? ''} error={error} />
      <Field label="Work arrangement" name="workArrangement" maxLength={200} defaultValue={role?.workArrangement ?? ''} error={error} />
    </div>
    <Field label="Salary" name="salaryText" maxLength={200} defaultValue={role?.salaryText ?? ''} error={error} />
    <TextArea label="Role notes" name="notes" defaultValue={role?.notes} error={error} />
    <div className="form-actions">
      <button className="button" type="submit" disabled={write.isPending}>{write.isPending ? 'Saving…' : role ? 'Save details' : 'Save role'}</button>
      {role ? <button type="button" className="button secondary" onClick={onClose}>Cancel</button> :
        <Link className="button secondary" to="/roles">Cancel</Link>}
    </div>
  </form>;
}

export function RoleFacts({ role }: { role: Role }) {
  return <>
    <dl className="facts">
      <div><dt>Company</dt><dd>{role.company}</dd></div>
      <div><dt>Role title</dt><dd>{role.title}</dd></div>
      <div><dt>Location</dt><dd>{role.location || '—'}</dd></div>
      <div><dt>Work arrangement</dt><dd>{role.workArrangement || '—'}</dd></div>
      <div><dt>Salary</dt><dd>{role.salaryText || '—'}</dd></div>
      <div><dt>Posting</dt><dd>{role.postingUrl ? <a href={role.postingUrl} target="_blank" rel="noreferrer">Open posting</a> : '—'}</dd></div>
    </dl>
    <h3>Role notes</h3><p className="preserve">{role.notes || 'No notes yet.'}</p>
  </>;
}

export function NewRolePage() {
  return <><PageHeading title="Add role" /><RoleForm /></>;
}
