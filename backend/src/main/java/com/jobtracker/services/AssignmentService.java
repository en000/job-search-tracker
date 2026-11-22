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

@Service
public class AssignmentService {
    private final RoleWorkflow workflow;
    private final AssignmentRepository assignments;
    private final RoleEventService events;
    private final Clock clock;

    AssignmentService(RoleWorkflow workflow, AssignmentRepository assignments, RoleEventService events, Clock clock) {
        this.workflow = workflow;
        this.assignments = assignments;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<AssignmentDetails> listAssignments(UUID roleId) {
        workflow.read(roleId);
        return assignments.listScoped(roleId).stream().map(ViewMapper::assignment).toList();
    }

    @Transactional(readOnly = true)
    public AssignmentDetails getAssignment(UUID roleId, UUID assignmentId) {
        return ViewMapper.assignment(assignment(roleId, assignmentId));
    }

    @Transactional
    public AssignmentDetails createAssignment(UUID roleId, AssignmentInput input) {
        Role role = workflow.lock(roleId);
        RoleWorkflow.requireActive(role);
        Instant now = now(clock);
        Assignment assignment = new Assignment();
        assignment.id = UUID.randomUUID();
        assignment.role = role;
        assignment.createdAt = now;
        assignment.updatedAt = now;
        applyAssignment(assignment, input, input.status() == null ? AssignmentStatus.PENDING : input.status(), now);
        assignments.saveAndFlush(assignment);
        RoleWorkflow.touch(role, now);
        events.childEvent(role, EventKind.ASSIGNMENT_CREATED, null, assignment, now,
                Map.of("title", assignment.title, "after", assignment.status));
        return ViewMapper.assignment(assignment);
    }

    @Transactional
    public AssignmentDetails updateAssignment(UUID roleId, UUID assignmentId, AssignmentInput input) {
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
            events.childEvent(role, EventKind.ASSIGNMENT_STATUS_CHANGED, null, assignment, now,
                    Map.of("title", assignment.title, "before", before.status(), "after", assignment.status));
        }
        assignments.flush();
        return ViewMapper.assignment(assignment);
    }

    private Assignment assignment(UUID roleId, UUID assignmentId) {
        return assignments.findScoped(roleId, assignmentId)
                .orElseThrow(() -> missing("Assignment"));
    }

    private static void applyAssignment(Assignment assignment, AssignmentInput input, AssignmentStatus status,
                                        Instant now) {
        assignment.title = required(input.title(), "title");
        assignment.status = status;
        assignment.receivedOn = input.receivedOn();
        assignment.dueOn = input.dueOn();
        assignment.instructions = optional(input.instructions());
        assignment.instructionsUrl = link(input.instructionsUrl(), "instructionsUrl");
        assignment.workUrl = link(input.workUrl(), "workUrl");
        assignment.notes = optional(input.notes());
        assignment.feedback = optional(input.feedback());
        if (input.submittedAt() != null) {
            assignment.submittedAt = timestamp(input.submittedAt());
        } else if (assignment.submittedAt == null && assignment.requiresSubmissionTime()) {
            assignment.submittedAt = now;
        }
    }
}
