export const stageCodes = ['SAVED', 'APPLIED', 'IN_PROGRESS', 'COMPLETED'] as const;
export type Stage = typeof stageCodes[number];

export const substageCodes = ['INTERVIEW_SCHEDULED', 'FIRST_INTERVIEW_PASSED', 'AWAITING_INTERVIEW_FEEDBACK', 'ASSIGNMENT_PENDING', 'ASSIGNMENT_SUBMITTED', 'FINAL_INTERVIEW', 'OFFER_RECEIVED'] as const;
export type Substage = typeof substageCodes[number];

export const outcomeCodes = ['ACCEPTED', 'REJECTED', 'WITHDRAWN', 'NO_RESPONSE'] as const;
export type Outcome = typeof outcomeCodes[number];

export const interviewTypeCodes = ['HR_RECRUITER', 'TECHNICAL', 'HIRING_MANAGER', 'PANEL', 'FINAL', 'OTHER'] as const;
export type InterviewType = typeof interviewTypeCodes[number];

export const interviewStatusCodes = ['PLANNED', 'SCHEDULED', 'AWAITING_FEEDBACK', 'PASSED', 'DID_NOT_PASS', 'CANCELLED'] as const;
export type InterviewStatus = typeof interviewStatusCodes[number];

export const assignmentStatusCodes = ['PENDING', 'IN_PROGRESS', 'SUBMITTED', 'PASSED', 'DID_NOT_PASS', 'CANCELLED'] as const;
export type AssignmentStatus = typeof assignmentStatusCodes[number];

export const eventKindCodes = ['ROLE_CREATED', 'ROLE_STAGE_CHANGED', 'ROLE_COMPLETED', 'ROLE_REOPENED', 'FOLLOW_UP_COMPLETED', 'INTERVIEW_CREATED', 'INTERVIEW_STATUS_CHANGED', 'ASSIGNMENT_CREATED', 'ASSIGNMENT_STATUS_CHANGED', 'NOTE', 'RECRUITER_CONVERSATION'] as const;
export type EventKind = typeof eventKindCodes[number];

export const todayKindCodes = ['FOLLOW_UP', 'ASSIGNMENT', 'SCHEDULED_INTERVIEW', 'PLANNED_INTERVIEW', 'RECORD_INTERVIEW_OUTCOME'] as const;
export type TodayKind = typeof todayKindCodes[number];

export const activeStages = ['SAVED', 'APPLIED', 'IN_PROGRESS'] as const;
export const substages: Partial<Record<Stage, readonly Substage[]>> = {
  IN_PROGRESS: ['INTERVIEW_SCHEDULED', 'FIRST_INTERVIEW_PASSED', 'AWAITING_INTERVIEW_FEEDBACK',
    'ASSIGNMENT_PENDING', 'ASSIGNMENT_SUBMITTED', 'FINAL_INTERVIEW', 'OFFER_RECEIVED'],
};

export type Page<T> = { items: T[]; page: number; size: number; total: number };
export type FollowUp = { text: string; dueOn: string | null };
export type RoleFields = {
  company: string;
  title: string;
  postingUrl: string | null;
  location: string | null;
  workArrangement: string | null;
  salaryText: string | null;
  notes: string | null;
};
export type Role = RoleFields & {
  id: string;
  stage: Stage;
  substage: Substage | null;
  previousStage: Stage | null;
  previousSubstage: Substage | null;
  outcome: Outcome | null;
  completedAt: string | null;
  followUp: FollowUp | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  lastActivityAt: string;
};

export type NextItem = {
  kind: TodayKind;
  childId: string | null;
  label: string;
  dueOn: string | null;
  startsAt: string | null;
};
export type RoleSummary = Pick<Role, 'id' | 'company' | 'title' | 'stage' | 'substage' | 'outcome' | 'lastActivityAt'> & {
  nextScheduledItem: NextItem | null;
};
export type Participant = { id?: string; name: string; jobTitle: string | null; email: string | null };
export type InterviewFields = {
  title: string;
  type: InterviewType;
  status: InterviewStatus;
  roundNumber: number | null;
  startsAt: string | null;
  durationMinutes: number | null;
  meetingUrl: string | null;
  location: string | null;
  prepNotes: string | null;
  interviewNotes: string | null;
  feedback: string | null;
  participants: Participant[];
};
type ChildRecord = { id: string; roleId: string; version: number; createdAt: string; updatedAt: string };
export type Interview = InterviewFields & ChildRecord;
export type AssignmentFields = {
  title: string;
  status: AssignmentStatus;
  receivedOn: string | null;
  dueOn: string | null;
  brief: string | null;
  briefUrl: string | null;
  workUrl: string | null;
  submissionUrl: string | null;
  workNotes: string | null;
  feedback: string | null;
  submittedAt: string | null;
};
export type Assignment = AssignmentFields & ChildRecord;
export type TimelineEvent = {
  id: string;
  interviewId: string | null;
  assignmentId: string | null;
  kind: EventKind;
  occurredAt: string;
  createdAt: string;
  body: string | null;
  details: Record<string, unknown>;
};
export type TodayItem = NextItem & {
  roleId: string;
  company: string;
  roleTitle: string;
  roleVersion: number | null;
};
export type CompletionSuggestion = {
  roleId: string;
  company: string;
  roleTitle: string;
  roleVersion: number;
};
export type Today = {
  localDate: string;
  timeZone: string;
  overdue: TodayItem[];
  dueToday: TodayItem[];
  upcoming: TodayItem[];
  unscheduled: TodayItem[];
  completionSuggestions: CompletionSuggestion[];
};
export type Settings = { timeZone: string };
