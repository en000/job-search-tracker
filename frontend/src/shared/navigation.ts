import type { Stage } from './types';

export type ReturnState = { rolesUrl?: string; rolesScroll?: number };
export type InterviewStageSuggestion = { stage: Stage | null };
export type RoleNavigationState = ReturnState & { interviewStageSuggestion?: InterviewStageSuggestion };

export function returnState(state: unknown): ReturnState {
  if (!state || typeof state !== 'object') return {};
  const { rolesUrl, rolesScroll } = state as ReturnState;
  return { rolesUrl, rolesScroll };
}
