package com.jobtracker.services;

import java.time.*;
import java.util.*;
import com.jobtracker.domain.*;
import com.jobtracker.domain.enums.*;
import com.jobtracker.dtos.requests.*;
import com.jobtracker.dtos.responses.*;
import com.jobtracker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import static com.jobtracker.services.Inputs.*;
import static com.jobtracker.services.TrackerException.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Service
public class InterviewService {
    private final RoleWorkflow workflow;
    private final InterviewRepository interviews;
    private final ParticipantService participants;
    private final RoleEventService events;
    private final EntityManager entityManager;
    private final Clock clock;

    InterviewService(RoleWorkflow workflow, InterviewRepository interviews, ParticipantService participants,
                     RoleEventService events, EntityManager entityManager, Clock clock) {
        this.workflow = workflow;
        this.interviews = interviews;
        this.participants = participants;
        this.events = events;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<InterviewDetails> listInterviews(UUID roleId) {
        workflow.read(roleId);
        return interviews.listScoped(roleId).stream().map(this::interviewView).toList();
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public InterviewDetails getInterview(UUID roleId, UUID interviewId) {
        return interviewView(interview(roleId, interviewId));
    }

    @Transactional
    public InterviewDetails createInterview(UUID roleId, InterviewInput input) {
        Role role = workflow.lock(roleId);
        RoleWorkflow.requireActive(role);
        Instant now = now(clock);
        Interview interview = new Interview();
        interview.id = UUID.randomUUID();
        interview.role = role;
        interview.createdAt = now;
        interview.updatedAt = now;
        applyInterview(interview, input, input.status() == null ? InterviewStatus.PLANNED : input.status());
        interviews.saveAndFlush(interview);
        List<ParticipantDetails> people = participants.replaceParticipants(interview, List.of(), input.participants());
        RoleWorkflow.touch(role, now);
        events.childEvent(role, EventKind.INTERVIEW_CREATED, interview, null, now,
                Map.of("title", interview.title, "after", interview.status));
        return ViewMapper.interview(interview, people);
    }

    @Transactional
    public InterviewDetails updateInterview(UUID roleId, UUID interviewId, InterviewInput input) {
        Role role = workflow.lock(roleId);
        Interview interview = interview(roleId, interviewId);
        checkVersion(input.expectedVersion(), interview.version);
        if (input.status() == null) throw invalid("status", "Choose a status.");
        List<Participant> previous = participants.records(interview);
        List<ParticipantDetails> previousPeople = previous.stream().map(ViewMapper::participant).toList();
        InterviewDetails before = ViewMapper.interview(interview, previousPeople);
        applyInterview(interview, input, input.status());
        boolean fieldsChanged = !before.equals(ViewMapper.interview(interview, previousPeople));
        List<ParticipantDetails> people = participants.replaceParticipants(interview, previous, input.participants());
        if (!fieldsChanged && previousPeople.equals(people)) return before;

        Instant now = now(clock);
        if (!fieldsChanged && now.equals(interview.updatedAt)) {
            // A participant-only edit needs a version change even when the timestamp is identical.
            entityManager.lock(interview, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        }
        interview.updatedAt = now;
        RoleWorkflow.touch(role, now);
        if (before.status() != interview.status) {
            events.childEvent(role, EventKind.INTERVIEW_STATUS_CHANGED, interview, null, now,
                    Map.of("title", interview.title, "before", before.status(), "after", interview.status));
        }
        interviews.flush();
        return ViewMapper.interview(interview, people);
    }

    private Interview interview(UUID roleId, UUID interviewId) {
        return interviews.findScoped(roleId, interviewId)
                .orElseThrow(() -> missing("Interview"));
    }

    private InterviewDetails interviewView(Interview interview) {
        var people = participants.details(interview);
        return ViewMapper.interview(interview, people);
    }

    private static void applyInterview(Interview interview, InterviewInput input, InterviewStatus status) {
        if (status == InterviewStatus.SCHEDULED && input.startsAt() == null) {
            throw invalid("startsAt", "Enter a start time for a scheduled interview.");
        }
        interview.title = required(input.title(), "title");
        interview.type = input.type();
        interview.status = status;
        interview.roundNumber = input.roundNumber();
        interview.startsAt = timestamp(input.startsAt());
        interview.durationMinutes = input.durationMinutes();
        interview.meetingUrl = link(input.meetingUrl(), "meetingUrl");
        interview.location = optional(input.location());
        interview.prepNotes = optional(input.prepNotes());
        interview.interviewNotes = optional(input.interviewNotes());
        interview.feedback = optional(input.feedback());
    }

}
