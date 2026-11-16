package local.jobtracker.services;

import java.time.Instant;
import java.util.*;
import local.jobtracker.domain.Role;
import local.jobtracker.domain.RoleEvent;
import local.jobtracker.domain.Codes.EventKind;
import local.jobtracker.repository.EventRepository;
import local.jobtracker.repository.RoleRepository;
import org.springframework.stereotype.Component;

@Component
class RoleWorkflow {
    private final WorkspaceContext workspace;
    private final RoleRepository roles;
    private final EventRepository events;

    RoleWorkflow(WorkspaceContext workspace, RoleRepository roles, EventRepository events) {
        this.workspace = workspace;
        this.roles = roles;
        this.events = events;
    }

    Role read(UUID id) {
        return roles.findByWorkspaceIdAndId(workspace.id(), id)
                .orElseThrow(() -> TrackerException.missing("Role"));
    }

    Role lock(UUID id) {
        return roles.lock(workspace.id(), id).orElseThrow(() -> TrackerException.missing("Role"));
    }

    static void requireActive(Role role) {
        if (!role.isActive()) throw TrackerException.conflict("role_completed", "Reopen this role to make that change.");
    }

    static void touch(Role role, Instant now) {
        role.lastActivityAt = now;
    }

    RoleEvent event(Role role, EventKind kind, Instant now, Map<String, Object> details) {
        return event(role, kind, null, null, now, now, null, details);
    }

    RoleEvent childEvent(Role role, EventKind kind, UUID interviewId, UUID assignmentId, Instant now,
                         Map<String, Object> details) {
        return event(role, kind, interviewId, assignmentId, now, now, null, details);
    }

    RoleEvent manualEvent(Role role, EventKind kind, Instant now, Instant occurredAt, String body) {
        return event(role, kind, null, null, now, occurredAt, body, Map.of());
    }

    private RoleEvent event(Role role, EventKind kind, UUID interviewId, UUID assignmentId, Instant now,
                            Instant occurredAt, String body, Map<String, Object> details) {
        RoleEvent event = new RoleEvent();
        event.id = UUID.randomUUID();
        event.roleId = role.id;
        event.kind = kind;
        event.interviewId = interviewId;
        event.assignmentId = assignmentId;
        event.occurredAt = occurredAt;
        event.createdAt = now;
        event.body = body;
        event.details = details;
        return events.save(event);
    }

    static Map<String, Object> snapshot(Role role) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("stage", role.stage);
        state.put("substage", role.substage);
        state.put("outcome", role.outcome);
        return state;
    }
}
