import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { queries } from '../../shared/api';
import { formatDate, formatInstant } from '../../shared/dates';
import { label } from '../../shared/labels';
import { returnState, type RoleNavigationState } from '../../shared/navigation';
import { LoadState, PageHeading, Pill } from '../../shared/ui';
import { FollowUpPanel, RoleProgress } from './RoleActions';
import { RoleFacts, RoleForm } from './RoleForm';
import { Timeline } from './Timeline';

export function RolePage() {
  const roleId = useParams().roleId!;
  const location = useLocation();
  const navigate = useNavigate();
  const state = returnState(location.state);
  const interviewStageSuggestion = (location.state as RoleNavigationState | null)?.interviewStageSuggestion;
  const reviewNoResponse = new URLSearchParams(location.search).get('review') === 'no-response';
  const rolePath = '/roles/' + roleId;
  const result = useQuery(queries.role(roleId));
  const settings = useQuery(queries.settings());
  const interviews = useQuery({ ...queries.interviews(roleId), enabled: !!result.data });
  const assignments = useQuery({ ...queries.assignments(roleId), enabled: !!result.data });
  const [editing, setEditing] = useState(false);

  const role = result.data;
  const zone = settings.data?.timeZone;
  if (!role || !zone) return <>
    <Link className="back-link" to={state.rolesUrl ?? '/roles'}>← Roles</Link>
    <LoadState error={result.error ?? settings.error} retry={() => { void result.refetch(); void settings.refetch(); }} />
  </>;

  const rolesUrl = state.rolesUrl ?? '/roles';
  return <>
    <Link className="back-link" to={rolesUrl} state={{
      rolesScroll: state.rolesUrl ? Number(sessionStorage.getItem('roles-scroll:' + rolesUrl) ?? 0) : 0,
    }}>← Roles</Link>
    <PageHeading title={role.title} caption={role.company} />
    <RoleProgress key={role.id + ':' + reviewNoResponse} role={role} zone={zone} reviewNoResponse={reviewNoResponse}
      interviewStageSuggestion={interviewStageSuggestion}
      onDismissInterviewSuggestion={() => navigate(rolePath + location.search, { replace: true, state })} />
    <div className="role-layout">
      <aside><FollowUpPanel key={role.id} role={role} /></aside>
      <div className="role-main">
        <section className="surface panel">
          <div className="section-heading"><h2>Interviews</h2>
            {role.status !== 'COMPLETED' && <Link className="button secondary small" state={state}
              to={rolePath + '/interviews/new'}>Add interview</Link>}</div>
          {!interviews.data ? <LoadState error={interviews.error} retry={interviews.refetch} /> :
            interviews.data.length === 0 ? <p className="muted">No interviews yet.</p> :
              <ul className="child-list">{interviews.data.map((item) => <li key={item.id}>
                <div><Link className="work-label" state={state} to={rolePath + '/interviews/' + item.id}>{item.title}</Link>
                  <div className="muted">{label(item.type)}{item.startsAt && ' · ' + formatInstant(item.startsAt, zone)}</div></div>
                <Pill code={item.status} />
              </li>)}</ul>}
        </section>
        <section className="surface panel">
          <div className="section-heading"><h2>Assignments</h2>
            {role.status !== 'COMPLETED' && <Link className="button secondary small" state={state}
              to={rolePath + '/assignments/new'}>Add assignment</Link>}</div>
          {!assignments.data ? <LoadState error={assignments.error} retry={assignments.refetch} /> :
            assignments.data.length === 0 ? <p className="muted">No assignments yet.</p> :
              <ul className="child-list">{assignments.data.map((item) => <li key={item.id}>
                <div><Link className="work-label" state={state} to={rolePath + '/assignments/' + item.id}>{item.title}</Link>
                  {item.dueOn && <div className="muted">Due {formatDate(item.dueOn)}</div>}</div>
                <Pill code={item.status} />
              </li>)}</ul>}
        </section>
        <section className="surface panel">
          <div className="section-heading"><h2>Role details</h2>
            {!editing && <button className="button secondary small" onClick={() => setEditing(true)}>Edit details</button>}</div>
          {editing ? <RoleForm key={role.id} role={role} onClose={() => setEditing(false)} /> : <RoleFacts role={role} />}
        </section>
        <Timeline key={role.id} roleId={role.id} zone={zone} state={state} />
      </div>
    </div>
  </>;
}
