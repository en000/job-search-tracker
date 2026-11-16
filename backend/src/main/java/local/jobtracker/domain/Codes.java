package local.jobtracker.domain;

public final class Codes {
    private Codes() {}

    public enum Stage {
        SAVED,
        APPLIED,
        IN_PROGRESS,
        COMPLETED
    }

    public enum Substage {
        INTERVIEW_SCHEDULED,
        FIRST_INTERVIEW_PASSED,
        AWAITING_INTERVIEW_FEEDBACK,
        ASSIGNMENT_PENDING,
        ASSIGNMENT_SUBMITTED,
        FINAL_INTERVIEW,
        OFFER_RECEIVED
    }

    public enum Outcome {
        ACCEPTED,
        REJECTED,
        WITHDRAWN,
        NO_RESPONSE
    }

    public enum InterviewType {
        HR_RECRUITER,
        TECHNICAL,
        HIRING_MANAGER,
        PANEL,
        FINAL,
        OTHER
    }

    public enum InterviewStatus {
        PLANNED,
        SCHEDULED,
        AWAITING_FEEDBACK,
        PASSED,
        DID_NOT_PASS,
        CANCELLED
    }

    public enum AssignmentStatus {
        PENDING,
        IN_PROGRESS,
        SUBMITTED,
        PASSED,
        DID_NOT_PASS,
        CANCELLED
    }

    public enum EventKind {
        ROLE_CREATED,
        ROLE_STAGE_CHANGED,
        ROLE_COMPLETED,
        ROLE_REOPENED,
        FOLLOW_UP_COMPLETED,
        INTERVIEW_CREATED,
        INTERVIEW_STATUS_CHANGED,
        ASSIGNMENT_CREATED,
        ASSIGNMENT_STATUS_CHANGED,
        NOTE,
        RECRUITER_CONVERSATION
    }

    public enum TodayKind {
        FOLLOW_UP,
        ASSIGNMENT,
        SCHEDULED_INTERVIEW,
        PLANNED_INTERVIEW,
        RECORD_INTERVIEW_OUTCOME
    }
}
