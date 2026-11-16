import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StrictMode, useLayoutEffect } from 'react';
import { createRoot } from 'react-dom/client';
import { createBrowserRouter, Link, Navigate, NavLink, Outlet, RouterProvider, useLocation, useRouteError } from 'react-router-dom';
import { NewRolePage } from '../features/roles/RoleForm';
import { RolesPage } from '../features/roles/RolesPage';
import { SettingsPage } from '../features/settings/SettingsPage';
import { TodayPage } from '../features/today/TodayPage';
import './styles.css';

function Shell() {
  const location = useLocation();
  useLayoutEffect(() => {
    if (location.pathname !== '/roles') window.scrollTo(0, 0);
  }, [location.pathname]);

  return <>
    <a className="skip-link" href="#main">Skip to content</a>
    <header className="site-header">
      <div className="header-inner">
        <Link className="brand" to="/today"><span className="brand-mark" aria-hidden="true"><i /></span>Job Search Tracker</Link>
        <nav className="site-nav" aria-label="Main">
          <NavLink to="/today">Today</NavLink>
          <NavLink to="/roles" className={location.pathname === '/roles/new' ? 'ignore-active' : undefined}>Roles</NavLink>
          <NavLink to="/settings">Settings</NavLink>
          <NavLink className="button" to="/roles/new"><span aria-hidden="true">＋</span> Add role</NavLink>
        </nav>
      </div>
    </header>
    <main id="main" tabIndex={-1}><Outlet /></main>
  </>;
}

function PageError() {
  const error = useRouteError();
  return <main className="page-failure"><h1>Something went wrong</h1>
    <p>{error instanceof Error ? error.message : 'Try opening the tracker again.'}</p>
    <a href="/today" className="button">Go to Today</a></main>;
}

const interviewRoute = async () => ({ Component: (await import('../features/interviews/InterviewPage')).InterviewPage });
const assignmentRoute = async () => ({ Component: (await import('../features/assignments/AssignmentPage')).AssignmentPage });

const router = createBrowserRouter([{
  element: <Shell />,
  errorElement: <PageError />,
  hydrateFallbackElement: <main><p className="surface empty" role="status">Loading…</p></main>,
  children: [
    { index: true, element: <Navigate to="/today" replace /> },
    { path: 'today', element: <TodayPage /> },
    { path: 'roles', element: <RolesPage /> },
    { path: 'roles/new', element: <NewRolePage /> },
    { path: 'roles/:roleId', lazy: async () => ({ Component: (await import('../features/roles/RolePage')).RolePage }) },
    { path: 'roles/:roleId/interviews/new', lazy: interviewRoute },
    { path: 'roles/:roleId/interviews/:interviewId', lazy: interviewRoute },
    { path: 'roles/:roleId/assignments/new', lazy: assignmentRoute },
    { path: 'roles/:roleId/assignments/:assignmentId', lazy: assignmentRoute },
    { path: 'settings', element: <SettingsPage /> },
    { path: '*', element: <section className="surface empty"><h1>Page not found</h1><Link to="/today">Go to Today</Link></section> },
  ],
}]);

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
createRoot(document.getElementById('root')!).render(
  <StrictMode><QueryClientProvider client={queryClient}><RouterProvider router={router} /></QueryClientProvider></StrictMode>,
);
