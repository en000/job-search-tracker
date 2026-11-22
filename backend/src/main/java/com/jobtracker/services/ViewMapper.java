package com.jobtracker.services;

import java.util.List;
import com.jobtracker.domain.*;
import com.jobtracker.dtos.responses.*;

final class ViewMapper {
    private ViewMapper() {}

    static RoleDetails role(Role role) {
        FollowUp followUp = role.nextActionText == null ? null : new FollowUp(role.nextActionText, role.nextActionDueOn);
        return new RoleDetails(role.id, role.company, role.title, role.postingUrl, role.location, role.workArrangement,
                role.salaryText, role.notes, role.status, role.stage,
                role.outcome, role.completedAt, followUp, role.version, role.createdAt, role.updatedAt, role.lastActivityAt,
                role.stageChangedAt, role.completionSuggestionDismissed, role.completionSuggestionSnoozedUntil);
    }

    static ParticipantDetails participant(Participant participant) {
        return new ParticipantDetails(participant.id, participant.name, participant.jobTitle, participant.email);
    }

    static InterviewDetails interview(Interview interview, List<ParticipantDetails> people) {
        return new InterviewDetails(interview.id, interview.role.getId(), interview.title, interview.type, interview.status,
                interview.roundNumber, interview.startsAt, interview.durationMinutes, interview.meetingUrl,
                interview.location, interview.prepNotes, interview.interviewNotes, interview.feedback, people,
                interview.version, interview.createdAt, interview.updatedAt);
    }

    static AssignmentDetails assignment(Assignment assignment) {
        return new AssignmentDetails(assignment.id, assignment.role.getId(), assignment.title, assignment.status,
                assignment.receivedOn, assignment.dueOn, assignment.instructions, assignment.instructionsUrl, assignment.workUrl,
                assignment.notes, assignment.feedback, assignment.submittedAt,
                assignment.version, assignment.createdAt, assignment.updatedAt);
    }

    static EventDetails event(RoleEvent event) {
        return new EventDetails(event.id, event.role.getId(), event.interview == null ? null : event.interview.getId(),
                event.assignment == null ? null : event.assignment.getId(), event.kind,
                event.occurredAt, event.createdAt, event.body, event.details);
    }
}
