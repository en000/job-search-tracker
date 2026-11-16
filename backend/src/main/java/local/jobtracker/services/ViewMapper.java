package local.jobtracker.services;

import java.util.List;
import local.jobtracker.domain.*;
import local.jobtracker.dtos.Responses.*;

final class ViewMapper {
    private ViewMapper() {}

    static RoleDetails role(Role role) {
        FollowUp followUp = role.nextActionText == null ? null : new FollowUp(role.nextActionText, role.nextActionDueOn);
        return new RoleDetails(role.id, role.company, role.title, role.postingUrl, role.location, role.workArrangement,
                role.salaryText, role.notes, role.stage, role.substage, role.previousStage, role.previousSubstage,
                role.outcome, role.completedAt, followUp, role.version, role.createdAt, role.updatedAt, role.lastActivityAt,
                role.stageChangedAt, role.completionSuggestionDismissed, role.completionSuggestionSnoozedUntil);
    }

    static ParticipantDetails participant(Participant participant) {
        return new ParticipantDetails(participant.id, participant.name, participant.jobTitle, participant.email);
    }

    static InterviewDetails interview(Interview interview, List<ParticipantDetails> people) {
        return new InterviewDetails(interview.id, interview.roleId, interview.title, interview.type, interview.status,
                interview.roundNumber, interview.startsAt, interview.durationMinutes, interview.meetingUrl,
                interview.location, interview.prepNotes, interview.interviewNotes, interview.feedback, people,
                interview.version, interview.createdAt, interview.updatedAt);
    }

    static AssignmentDetails assignment(Assignment assignment) {
        return new AssignmentDetails(assignment.id, assignment.roleId, assignment.title, assignment.status,
                assignment.receivedOn, assignment.dueOn, assignment.brief, assignment.briefUrl, assignment.workUrl,
                assignment.submissionUrl, assignment.workNotes, assignment.feedback, assignment.submittedAt,
                assignment.version, assignment.createdAt, assignment.updatedAt);
    }

    static EventDetails event(RoleEvent event) {
        return new EventDetails(event.id, event.roleId, event.interviewId, event.assignmentId, event.kind,
                event.occurredAt, event.createdAt, event.body, event.details);
    }
}
