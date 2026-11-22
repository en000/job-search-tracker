const names: Record<string, string> = {
  HR_RECRUITER: 'HR / recruiter screen',
  ROLE_CREATED: 'Role added',
  ROLE_STAGE_CHANGED: 'Progress changed',
  ROLE_COMPLETED: 'Role completed',
  ROLE_REOPENED: 'Role reopened',
  FOLLOW_UP: 'Follow-up',
  FOLLOW_UP_COMPLETED: 'Follow-up completed',
  INTERVIEW_CREATED: 'Interview added',
  INTERVIEW_STATUS_CHANGED: 'Interview status changed',
  ASSIGNMENT_CREATED: 'Assignment added',
  ASSIGNMENT_STATUS_CHANGED: 'Assignment status changed',
};

export function label(code: string) {
  return names[code] ?? code.charAt(0) + code.slice(1).toLowerCase().replaceAll('_', ' ');
}
