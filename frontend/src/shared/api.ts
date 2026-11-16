import { queryOptions, useMutation, useQueryClient } from '@tanstack/react-query';
import type { Assignment, Interview, Page, Role, RoleSummary, Settings, TimelineEvent, Today } from './types';

type Problem = { status?: number; detail?: string; code?: string; fieldErrors?: Record<string, string> };

export class ApiError extends Error {
  constructor(public status: number, public problem: Problem) {
    super(problem.detail ?? 'The request could not be completed.');
    this.name = 'ApiError';
  }

  get stale() {
    return this.problem.code === 'stale_version';
  }
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch('/api' + path, options);
  if (!response.ok) {
    const problem = await response.json().catch(() => ({})) as Problem;
    throw new ApiError(response.status, problem);
  }
  return response.json() as Promise<T>;
}

export function send<T>(path: string, method: 'POST' | 'PUT' | 'PATCH', data: unknown) {
  return request<T>(path, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
}

export const queries = {
  today: () => queryOptions({ queryKey: ['tracker', 'today'], queryFn: () => request<Today>('/today') }),
  roles: (search = '') => queryOptions({
    queryKey: ['tracker', 'roles', search],
    queryFn: () => request<Page<RoleSummary>>('/roles?' + search),
  }),
  role: (roleId: string) => queryOptions({
    queryKey: ['tracker', 'role', roleId],
    queryFn: () => request<Role>('/roles/' + roleId),
  }),
  interviews: (roleId: string) => queryOptions({
    queryKey: ['tracker', 'interviews', roleId],
    queryFn: () => request<Interview[]>('/roles/' + roleId + '/interviews'),
  }),
  interview: (roleId: string, id: string) => queryOptions({
    queryKey: ['tracker', 'interview', roleId, id],
    queryFn: () => request<Interview>('/roles/' + roleId + '/interviews/' + id),
  }),
  assignments: (roleId: string) => queryOptions({
    queryKey: ['tracker', 'assignments', roleId],
    queryFn: () => request<Assignment[]>('/roles/' + roleId + '/assignments'),
  }),
  assignment: (roleId: string, id: string) => queryOptions({
    queryKey: ['tracker', 'assignment', roleId, id],
    queryFn: () => request<Assignment>('/roles/' + roleId + '/assignments/' + id),
  }),
  timeline: (roleId: string, page: number) => queryOptions({
    queryKey: ['tracker', 'timeline', roleId, page],
    queryFn: () => request<Page<TimelineEvent>>('/roles/' + roleId + '/timeline?page=' + page),
  }),
  settings: () => queryOptions({
    queryKey: ['tracker', 'settings'],
    queryFn: () => request<Settings>('/settings'),
  }),
};

export function useWrite<T, Input>(submit: (input: Input) => Promise<T>) {
  const cache = useQueryClient();
  return useMutation({
    mutationFn: submit,
    retry: false,
    onSuccess: () => cache.invalidateQueries({ queryKey: ['tracker'] }),
    onError: (error) => {
      if (error instanceof ApiError && error.stale) {
        return cache.invalidateQueries({ queryKey: ['tracker'] });
      }
    },
  });
}

export function fieldError(error: unknown, field: string) {
  return error instanceof ApiError ? error.problem.fieldErrors?.[field] : undefined;
}
