package local.jobtracker.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import local.jobtracker.domain.*;
import local.jobtracker.dtos.Requests;
import local.jobtracker.dtos.Responses.*;
import local.jobtracker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import static local.jobtracker.domain.Codes.*;
import static local.jobtracker.services.Inputs.*;
import static local.jobtracker.services.TrackerException.*;

@Service
public class ChildService {
    private final WorkspaceContext workspace;
    private final RoleWorkflow workflow;
    private final InterviewRepository interviews;
    private final ParticipantRepository participants;
    private final AssignmentRepository assignments;
    private final EntityManager entityManager;
    private final Clock clock;

    ChildService(WorkspaceContext workspace, RoleWorkflow workflow, InterviewRepository interviews,
                 ParticipantRepository participants, AssignmentRepository assignments, EntityManager entityManager,
                 Clock clock) {
        this.workspace = workspace;
        this.workflow = workflow;
        this.interviews = interviews;
        this.participants = participants;
        this.assignments = assignments;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<InterviewDetails> listInterviews(UUID roleId) {
        workflow.read(roleId);
        return interviews.listScoped(workspace.id(), roleId).stream().map(this::interviewView).toList();
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public InterviewDetails getInterview(UUID roleId, UUID interviewId) {
        return interviewView(interview(roleId, interviewId));
    }

    @Transactional
    public InterviewDetails createInterview(UUID roleId, Requests.InterviewInput input) {
        Role role = workflow.lock(roleId);
        RoleWorkflow.requireActive(role);
        Instant now = now(clock);
        Interview interview = new Interview();
        interview.id = UUID.randomUUID();
        interview.roleId = role.id;
        interview.createdAt = now;
        interview.updatedAt = now;
        applyInterview(interview, input, input.status() == null ? InterviewStatus.PLANNED : input.status());
        interviews.saveAndFlush(interview);
        List<ParticipantDetails> people = replaceParticipants(interview, List.of(), input.participants());
        RoleWorkflow.touch(role, now);
        workflow.childEvent(role, EventKind.INTERVIEW_CREATED, interview.id, null, now,
                Map.of("title", interview.title, "after", interview.status));
        return ViewMapper.interview(interview, people);
    }

    @Transactional
    public InterviewDetails updateInterview(UUID roleId, UUID interviewId, Requests.InterviewInput input) {
        Role role = workflow.lock(roleId);
        Interview interview = interview(roleId, interviewId);
        checkVersion(input.expectedVersion(), interview.version);
        if (input.status() == null) throw invalid("status", "Choose a status.");
        List<Participant> previous = participants.listScoped(workspace.id(), role.id, interview.id);
        List<ParticipantDetails> previousPeople = previous.stream().map(ViewMapper::participant).toList();
        InterviewDetails before = ViewMapper.interview(interview, previousPeople);
        applyInterview(interview, input, input.status());
        boolean fieldsChanged = !before.equals(ViewMapper.interview(interview, previousPeople));
        List<ParticipantDetails> people = replaceParticipants(interview, previous, input.participants());
        if (!fieldsChanged && previousPeople.equals(people)) return before;

        Instant now = now(clock);
        if (!fieldsChanged && now.equals(interview.updatedAt)) {
            // A participant-only edit needs a version change even when the timestamp is identical.
            entityManager.lock(interview, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        }
        interview.updatedAt = now;
        RoleWorkflow.touch(role, now);
        if (before.status() != interview.status) {
            workflow.childEvent(role, EventKind.INTERVIEW_STATUS_CHANGED, interview.id, null, now,
                    Map.of("title", interview.title, "before", before.status(), "after", interview.status));
        }
        interviews.flush();
        return ViewMapper.interview(interview, people);
    }

    private Interview interview(UUID roleId, UUID interviewId) {
        return interviews.findScoped(workspace.id(), roleId, interviewId)
                .orElseThrow(() -> missing("Interview"));
    }

    private InterviewDetails interviewView(Interview interview) {
        var people = participants.listScoped(workspace.id(), interview.roleId, interview.id)
                .stream().map(ViewMapper::participant).toList();
        return ViewMapper.interview(interview, people);
    }

    private static void applyInterview(Interview interview, Requests.InterviewInput input, InterviewStatus status) {
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

    private List<ParticipantDetails> replaceParticipants(Interview interview, List<Participant> previous,
                                                         List<Requests.ParticipantInput> inputs) {
        Map<UUID, Participant> remaining = previous.stream()
                .collect(Collectors.toMap(person -> person.id, Function.identity()));
        List<Requests.ParticipantInput> requested = inputs == null ? List.of() : inputs;
        List<ParticipantDetails> result = new ArrayList<>(requested.size());
        for (int position = 0; position < requested.size(); position++) {
            Requests.ParticipantInput input = requested.get(position);
            Participant person;
            if (input.id() == null) {
                person = new Participant();
                person.id = UUID.randomUUID();
                person.interviewId = interview.id;
            } else {
                person = remaining.remove(input.id());
                if (person == null) throw invalid("participants[" + position + "].id",
                        "This interviewer ID is duplicated or does not belong to this interview.");
            }
            person.position = position;
            person.name = required(input.name(), "participants[" + position + "].name");
            person.jobTitle = optional(input.jobTitle());
            person.email = optional(input.email());
            if (input.id() == null) entityManager.persist(person);
            result.add(ViewMapper.participant(person));
        }
        participants.deleteAll(remaining.values());
        return result;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<AssignmentDetails> listAssignments(UUID roleId) {
        workflow.read(roleId);
        return assignments.listScoped(workspace.id(), roleId).stream().map(ViewMapper::assignment).toList();
    }

    @Transactional(readOnly = true)
    public AssignmentDetails getAssignment(UUID roleId, UUID assignmentId) {
        return ViewMapper.assignment(assignment(roleId, assignmentId));
    }

    @Transactional
    public AssignmentDetails createAssignment(UUID roleId, Requests.AssignmentInput input) {
        Role role = workflow.lock(roleId);
        RoleWorkflow.requireActive(role);
        Instant now = now(clock);
        Assignment assignment = new Assignment();
        assignment.id = UUID.randomUUID();
        assignment.roleId = role.id;
        assignment.createdAt = now;
        assignment.updatedAt = now;
        applyAssignment(assignment, input, input.status() == null ? AssignmentStatus.PENDING : input.status(), now);
        assignments.saveAndFlush(assignment);
        RoleWorkflow.touch(role, now);
        workflow.childEvent(role, EventKind.ASSIGNMENT_CREATED, null, assignment.id, now,
                Map.of("title", assignment.title, "after", assignment.status));
        return ViewMapper.assignment(assignment);
    }

    @Transactional
    public AssignmentDetails updateAssignment(UUID roleId, UUID assignmentId, Requests.AssignmentInput input) {
        Role role = workflow.lock(roleId);
        Assignment assignment = assignment(roleId, assignmentId);
        checkVersion(input.expectedVersion(), assignment.version);
        if (input.status() == null) throw invalid("status", "Choose a status.");
        AssignmentDetails before = ViewMapper.assignment(assignment);
        Instant now = now(clock);
        applyAssignment(assignment, input, input.status(), now);
        if (before.equals(ViewMapper.assignment(assignment))) return before;

        assignment.updatedAt = now;
        RoleWorkflow.touch(role, now);
        if (before.status() != assignment.status) {
            workflow.childEvent(role, EventKind.ASSIGNMENT_STATUS_CHANGED, null, assignment.id, now,
                    Map.of("title", assignment.title, "before", before.status(), "after", assignment.status));
        }
        assignments.flush();
        return ViewMapper.assignment(assignment);
    }

    private Assignment assignment(UUID roleId, UUID assignmentId) {
        return assignments.findScoped(workspace.id(), roleId, assignmentId)
                .orElseThrow(() -> missing("Assignment"));
    }

    private static void applyAssignment(Assignment assignment, Requests.AssignmentInput input, AssignmentStatus status,
                                        Instant now) {
        assignment.title = required(input.title(), "title");
        assignment.status = status;
        assignment.receivedOn = input.receivedOn();
        assignment.dueOn = input.dueOn();
        assignment.brief = optional(input.brief());
        assignment.briefUrl = link(input.briefUrl(), "briefUrl");
        assignment.workUrl = link(input.workUrl(), "workUrl");
        assignment.submissionUrl = link(input.submissionUrl(), "submissionUrl");
        assignment.workNotes = optional(input.workNotes());
        assignment.feedback = optional(input.feedback());
        if (input.submittedAt() != null) {
            assignment.submittedAt = timestamp(input.submittedAt());
        } else if (assignment.submittedAt == null && assignment.requiresSubmissionTime()) {
            assignment.submittedAt = now;
        }
    }
}
