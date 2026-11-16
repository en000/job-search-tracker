import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiError, queries, send, useWrite } from '../../shared/api';
import { formatDate, formatInstant } from '../../shared/dates';
import { label } from '../../shared/labels';
import type { Role, Today, TodayItem } from '../../shared/types';
import { LoadState, PageHeading, WriteError } from '../../shared/ui';
import { CompletionSuggestions } from './CompletionSuggestions';

const sections = [
  { key: 'overdue', title: 'Overdue', empty: 'Nothing overdue.' },
  { key: 'dueToday', title: 'Due today', empty: 'Nothing due today.' },
  { key: 'upcoming', title: 'Upcoming', empty: 'No upcoming dates.' },
  { key: 'unscheduled', title: 'To do', empty: 'No to-dos without a date.' },
] as const;

function itemPath(item: TodayItem) {
  const role = '/roles/' + item.roleId;
  if (item.kind === 'FOLLOW_UP') return role;
  return role + (item.kind === 'ASSIGNMENT' ? '/assignments/' : '/interviews/') + item.childId;
}

export function TodayPage() {
  const result = useQuery(queries.today());
  const roles = useQuery(queries.roles('size=1'));
  const [pendingRole, setPendingRole] = useState<string>();
  const complete = useWrite((item: TodayItem) => send<Role>(
    '/roles/' + item.roleId + '/follow-up/complete', 'POST', { expectedVersion: item.roleVersion },
  ));

  if (!result.data) return <><PageHeading title="Today" /><LoadState error={result.error} retry={result.refetch} /></>;
  const today: Today = result.data;
  return <>
    <PageHeading title="Today" caption={formatDate(today.localDate, { dateStyle: 'full' })}>
      <Link to="/settings" className="zone-link">{today.timeZone}</Link>
    </PageHeading>
    {roles.data?.total === 0 && <section className="surface welcome">
      <div><h2>Start with a role</h2><p>Add a role to track applications, interviews, and follow-ups here.</p></div>
      <Link className="button" to="/roles/new">Add role</Link>
    </section>}
    <CompletionSuggestions suggestions={today.completionSuggestions} />
    <div className="today-grid">
      {sections.map((section) => <section className={'surface work-group ' + section.key} key={section.key}
        aria-labelledby={section.key + '-heading'}>
        <div className="section-heading">
          <h2 id={section.key + '-heading'}>{section.title}</h2>
          <span className="count" aria-label={today[section.key].length + ' items'}>{today[section.key].length}</span>
        </div>
        {today[section.key].length === 0 ? <p className="empty group-empty">{section.empty}</p> :
          <ul className="work-list">{today[section.key].map((item) =>
            <li key={item.kind + (item.childId ?? item.roleId)}>
              <div className="work-main">
                <Link to={itemPath(item)} className="work-label">{item.label}</Link>
                <div className="muted">{item.roleTitle} <span aria-hidden="true">·</span> {item.company}</div>
                <div className="work-meta">
                  <span>{label(item.kind)}</span>
                  {(item.startsAt || item.dueOn) && <time dateTime={item.startsAt ?? item.dueOn ?? undefined}>
                    {item.startsAt ? formatInstant(item.startsAt, today.timeZone) : formatDate(item.dueOn!)}
                  </time>}
                </div>
              </div>
              {item.kind === 'FOLLOW_UP' && <button className="button secondary small" disabled={complete.isPending}
                onClick={() => { setPendingRole(item.roleId); complete.mutate(item); }}>Complete follow-up</button>}
              {item.roleId === pendingRole && item.kind === 'FOLLOW_UP' && <div className="work-error">
                <WriteError error={complete.error} />
                {complete.error instanceof ApiError && complete.error.stale &&
                  <p className="muted">Review the refreshed follow-up above before completing it.</p>}
              </div>}
            </li>)}</ul>}
      </section>)}
    </div>
  </>;
}
