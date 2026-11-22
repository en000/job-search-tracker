import { useState, type FormEvent } from 'react';
import { send, useWrite } from '../../shared/api';
import { formatDate, formatInstant } from '../../shared/dates';
import { label } from '../../shared/labels';
import type { InterviewStageSuggestion } from '../../shared/navigation';
import type { Outcome, Role, RoleStatus, Stage } from '../../shared/types';
import { activeStatuses, outcomeCodes, stagesByStatus } from '../../shared/types';
import { Field, formText, nullableText, Pill, SelectField, TextArea, VersionReview, WriteError } from '../../shared/ui';

type Command = { action: string; method: 'PATCH' | 'POST' | 'PUT'; body: object };

function useRoleCommand(roleId: string) {
  return useWrite((command: Command) => send<Role>('/roles/' + roleId + command.action, command.method, command.body));
}

export function RoleProgress({ role, zone, reviewNoResponse, interviewStageSuggestion, onDismissInterviewSuggestion }: {
  role: Role; zone: string; reviewNoResponse: boolean;
  interviewStageSuggestion?: InterviewStageSuggestion; onDismissInterviewSuggestion: () => void;
}) {
  const write = useRoleCommand(role.id);
  const [progressDraft, setProgressDraft] = useState<{ status: RoleStatus; stage: Stage | '' }>();
  const [expected, setExpected] = useState<number | undefined>(
    reviewNoResponse && role.status !== 'COMPLETED' ? role.version : undefined,
  );
  const [completionOutcome, setCompletionOutcome] = useState<Outcome | undefined>(
    reviewNoResponse && role.status !== 'COMPLETED' ? 'NO_RESPONSE' : undefined,
  );
  const version = expected ?? role.version;
  const status = progressDraft?.status ?? role.status;
  const stage = progressDraft?.stage ?? role.stage ?? '';
  const available = stagesByStatus[status] ?? [];
  const showInterviewSuggestion = interviewStageSuggestion && role.status !== 'COMPLETED'
    && (role.status !== 'IN_PROGRESS' || (interviewStageSuggestion.stage !== null
      && role.stage !== interviewStageSuggestion.stage));

  function change(nextStatus: RoleStatus, nextStage: Stage | '') {
    setExpected(version);
    setProgressDraft({ status: nextStatus, stage: nextStage });
  }

  function clear() {
    setExpected(undefined);
    setProgressDraft(undefined);
    setCompletionOutcome(undefined);
    if (interviewStageSuggestion) onDismissInterviewSuggestion();
  }

  function run(action: string, method: 'POST' | 'PATCH', body: object) {
    if (write.isPending) return;
    setExpected(version);
    write.mutate({ action, method, body: { expectedVersion: version, ...body } }, { onSuccess: clear });
  }

  return <section className="surface status-panel" aria-label="Role progress">
    <div className="status-display">
      <Pill code={role.status} />
      {(role.stage || role.outcome) && <span>{label((role.stage ?? role.outcome)!)}</span>}
      {role.completedAt && <span className="muted">{formatInstant(role.completedAt, zone)}</span>}
    </div>
    <WriteError error={write.error} />
    <VersionReview error={write.error} current={role.version} expected={version}
      onApply={() => { setExpected(role.version); write.reset(); }}>
      <p>{label(role.status)}{role.stage && ' / ' + label(role.stage)}{role.outcome && ' / ' + label(role.outcome)}</p>
      {role.followUp && <p>Current follow-up: {role.followUp.text}</p>}
    </VersionReview>
    {showInterviewSuggestion && <div className="review form-stack">
      <p>Interview saved. Move this role to <strong>In progress
        {interviewStageSuggestion.stage && ' / ' + label(interviewStageSuggestion.stage)}</strong>?</p>
      <div className="form-actions">
        <button type="button" className="button small" disabled={write.isPending}
          onClick={() => run('', 'PATCH', { status: 'IN_PROGRESS', stage: interviewStageSuggestion.stage })}>
          Update progress
        </button>
        <button type="button" className="button secondary small" disabled={write.isPending}
          onClick={onDismissInterviewSuggestion}>Keep current progress</button>
      </div>
    </div>}
    {role.status === 'COMPLETED' ? <button type="button" className="button secondary" disabled={write.isPending}
      onClick={() => run('/reopen', 'POST', {})}>Reopen role</button> : <>
      <form className="status-form" onSubmit={(event) => {
        event.preventDefault();
        run('', 'PATCH', { status, stage: stage || null });
      }}>
        <SelectField label="Status" name="status" options={activeStatuses} value={status} error={write.error}
          onChange={(value) => { if (value) change(value, stage && stagesByStatus[value]?.includes(stage) ? stage : ''); }} />
        {available.length > 0 && <SelectField label="Stage" name="stage" options={available} empty="None"
          value={stage} onChange={(value) => change(status, value)} error={write.error} />}
        <button className="button secondary" type="submit" disabled={write.isPending || !progressDraft}>Save progress</button>
        {!completionOutcome && <button type="button" className="link-button" disabled={write.isPending}
          onClick={() => { setExpected(version); setCompletionOutcome('ACCEPTED'); }}>Complete role</button>}
      </form>
      {completionOutcome && <form className="complete-form form-stack" onSubmit={(event) => {
        event.preventDefault();
        run('/complete', 'POST', { outcome: formText(new FormData(event.currentTarget), 'outcome') });
      }}>
        <SelectField label="Outcome" name="outcome" options={outcomeCodes} defaultValue={completionOutcome} error={write.error} />
        {role.followUp && <p className="notice">Completing this role will clear the current follow-up: {role.followUp.text}</p>}
        <div className="form-actions">
          <button className="button" type="submit" disabled={write.isPending}>Confirm completion</button>
          <button className="button secondary" type="button" onClick={() => {
            setCompletionOutcome(undefined);
            if (!progressDraft) setExpected(undefined);
          }}>Cancel</button>
        </div>
      </form>}
    </>}
  </section>;
}

export function FollowUpPanel({ role }: { role: Role }) {
  const write = useRoleCommand(role.id);
  const [draft, setDraft] = useState<{ text: string; dueOn: string }>();
  const [expected, setExpected] = useState<number>();
  const version = expected ?? role.version;
  const followUp = role.followUp;

  function command(action: string, method: 'POST' | 'PUT', body?: object) {
    if (write.isPending) return;
    setExpected(version);
    write.mutate({ action, method, body: { expectedVersion: version, ...body } }, { onSuccess: () => {
      setDraft(undefined);
      setExpected(undefined);
    } });
  }

  function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    command('/follow-up', 'PUT', { followUp: { text: formText(form, 'followUp.text'), dueOn: nullableText(form, 'followUp.dueOn') } });
  }

  return <section className="surface panel followup">
    <h2>Follow-up</h2>
    <WriteError error={write.error} />
    <VersionReview error={write.error} current={role.version} expected={version}
      onApply={() => { setExpected(role.version); write.reset(); }}>
      <p>{followUp?.text ?? 'No current follow-up.'}</p>
      {followUp?.dueOn && <p>{formatDate(followUp.dueOn)}</p>}
    </VersionReview>
    {role.status === 'COMPLETED' ? <p className="muted">Reopen the role to set a follow-up.</p> : draft ? (
      <form className="form-stack" onSubmit={save}>
        <TextArea label="Action" name="followUp.text" defaultValue={draft.text} required error={write.error} />
        <Field label="Due date" name="followUp.dueOn" type="date" defaultValue={draft.dueOn} error={write.error} />
        <div className="form-actions">
          <button type="submit" className="button small" disabled={write.isPending}>Save follow-up</button>
          <button type="button" className="button secondary small" onClick={() => {
            setDraft(undefined); setExpected(undefined); write.reset();
          }}>Cancel</button>
        </div>
      </form>
    ) : <>
      <p className={followUp ? 'preserve followup-text' : 'muted'}>{followUp?.text ?? 'No current follow-up.'}</p>
      {followUp && <p className="muted"> {followUp.dueOn ? 'Due ' + formatDate(followUp.dueOn) : 'No due date'}</p>}
      <div className="form-actions">
        <button type="button" className="button secondary small" disabled={write.isPending} onClick={() => {
          setExpected(role.version); setDraft({ text: followUp?.text ?? '', dueOn: followUp?.dueOn ?? '' }); write.reset();
        }}>{followUp ? 'Edit follow-up' : 'Set follow-up'}</button>
        {followUp && <>
          <button type="button" className="button small" disabled={write.isPending}
            onClick={() => command('/follow-up/complete', 'POST')}>Complete follow-up</button>
          <button type="button" className="link-button" disabled={write.isPending}
            onClick={() => command('/follow-up', 'PUT', { followUp: null })}>Clear</button>
        </>}
      </div>
    </>}
  </section>;
}
