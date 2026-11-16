import { useQuery } from '@tanstack/react-query';
import { useLayoutEffect, useRef, type FormEvent } from 'react';
import { Link, useLocation, useNavigationType, useSearchParams } from 'react-router-dom';
import { queries } from '../../shared/api';
import { formatDate, formatInstant } from '../../shared/dates';
import { label } from '../../shared/labels';
import { returnState } from '../../shared/navigation';
import { outcomeCodes, stageCodes, substageCodes } from '../../shared/types';
import { Field, formText, LoadState, PageHeading, Pagination, Pill, SelectField } from '../../shared/ui';

const filters = ['q', 'stage', 'substage', 'outcome', 'sort'] as const;

export function RolesPage() {
  const [params, setParams] = useSearchParams();
  const location = useLocation();
  const navigation = useNavigationType();
  const result = useQuery(queries.roles(params.toString()));
  const settings = useQuery(queries.settings());
  const restored = useRef('');

  useLayoutEffect(() => {
    const state = returnState(location.state);
    const savedPosition = state.rolesScroll ?? (navigation === 'POP'
      ? Number(sessionStorage.getItem('roles-scroll:' + location.pathname + location.search) ?? 0)
      : 0);
    if (result.data && restored.current !== location.key) {
      restored.current = location.key;
      window.scrollTo(0, savedPosition);
    }
  }, [location, navigation, result.data]);

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    const next = new URLSearchParams();
    for (const key of filters) {
      const value = formText(values, key);
      if (value && (key !== 'sort' || value !== 'recent')) next.set(key, value);
    }
    setParams(next);
  }

  function setPage(page: number) {
    const next = new URLSearchParams(params);
    if (page) next.set('page', String(page));
    else next.delete('page');
    setParams(next);
    window.scrollTo(0, 0);
  }

  const activeFilters = filters.some((key) => key !== 'sort' && params.has(key));
  const zone = settings.data?.timeZone;
  const filterKey = filters.map((key) => params.get(key) ?? '').join('|');
  return <>
    <PageHeading title="Roles" />
    <form className="surface filters" onSubmit={submit} key={filterKey} role="search" aria-label="Filter roles">
      <Field name="q" label="Search" type="search" placeholder="Company or title" defaultValue={params.get('q') ?? ''} maxLength={200} />
      <SelectField name="stage" label="Stage" options={stageCodes} empty="All stages" defaultValue={params.get('stage') ?? ''} />
      <SelectField name="substage" label="Substage" options={substageCodes} empty="All substages" defaultValue={params.get('substage') ?? ''} />
      <SelectField name="outcome" label="Outcome" options={outcomeCodes} empty="All outcomes" defaultValue={params.get('outcome') ?? ''} />
      <label className="field"><span>Sort by</span><select name="sort" defaultValue={params.get('sort') ?? 'recent'}>
        <option value="recent">Recent activity</option><option value="nextScheduled">Next scheduled</option>
      </select></label>
      <button className="button" type="submit">Apply filters</button>
    </form>
    {!result.data || !zone ? <LoadState error={result.error ?? settings.error} retry={() => {
      void result.refetch(); void settings.refetch();
    }} /> : <>
      <div className="list-meta">
        <span>{result.data.total} {result.data.total === 1 ? 'role' : 'roles'}</span>
        {activeFilters && <button className="link-button" onClick={() => setParams({})}>Clear filters</button>}
      </div>
      {result.data.items.length ? <div className="surface role-table">
        <table>
          <thead><tr><th scope="col">Company and role</th><th scope="col">Status</th>
            <th scope="col">Next scheduled</th><th scope="col">Last activity</th></tr></thead>
          <tbody>{result.data.items.map((role) => {
            const item = role.nextScheduledItem;
            return <tr key={role.id}>
              <td><Link className="work-label" to={'/roles/' + role.id} state={{ rolesUrl: location.pathname + location.search }}
                onClick={(event) => {
                  if (!event.metaKey && !event.ctrlKey && !event.altKey && !event.shiftKey) {
                    sessionStorage.setItem('roles-scroll:' + location.pathname + location.search, String(window.scrollY));
                  }
                }}>{role.title}</Link><div className="muted">{role.company}</div></td>
              <td data-label="Status"><Pill code={role.stage} />
                {(role.substage || role.outcome) && <div className="muted detail-label">{label((role.substage ?? role.outcome)!)}</div>}</td>
              <td data-label="Next scheduled">{item ? <>
                <div className="next-item">{item.label}</div>
                <span className="muted">{label(item.kind)}</span>
                <div className="muted">{item.startsAt ? formatInstant(item.startsAt, zone) : formatDate(item.dueOn!)}</div>
              </> : <span className="muted">—</span>}</td>
              <td data-label="Last activity">{formatInstant(role.lastActivityAt, zone)}</td>
            </tr>;
          })}</tbody>
        </table>
      </div> : <div className="surface empty">
        <h2>{activeFilters ? 'No matching roles' : result.data.total ? 'No roles on this page' : 'No roles yet'}</h2>
        {!activeFilters && result.data.total === 0 && <Link className="button" to="/roles/new">Add role</Link>}
      </div>}
      <Pagination page={result.data.page} size={result.data.size} total={result.data.total} onPage={setPage} />
    </>}
  </>;
}
