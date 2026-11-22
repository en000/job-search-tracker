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
import org.springframework.data.domain.PageRequest;

@Service
public class RoleEventService {
    private final RoleWorkflow workflow;
    private final RoleEventRepository events;
    private final Clock clock;

    RoleEventService(RoleWorkflow workflow, RoleEventRepository events, Clock clock) {
        this.workflow = workflow;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<EventDetails> list(UUID roleId) {
        workflow.read(roleId);
        return events.listScoped(roleId).stream().map(ViewMapper::event).toList();
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<EventDetails> timeline(UUID id, int page, int size) {
        workflow.read(id);
        var result = events.pageScoped(id, PageRequest.of(page, size));
        return new Page<>(result.map(ViewMapper::event).getContent(), page, size, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public EventDetails timelineEvent(UUID roleId, UUID eventId) {
        return events.findScoped(roleId, eventId).map(ViewMapper::event)
                .orElseThrow(() -> missing("Timeline event"));
    }

    @Transactional
    public EventDetails log(UUID id, EventInput request) {
        Role role = workflow.lock(id);
        if (request.kind() != EventKind.NOTE && request.kind() != EventKind.RECRUITER_CONVERSATION) {
            throw invalid("kind", "Choose Note or Recruiter conversation.");
        }
        Instant now = now(clock);
        Instant occurredAt = request.occurredAt() == null ? now : timestamp(request.occurredAt());
        RoleEvent event = manualEvent(role, request.kind(), now, occurredAt, required(request.body(), "body"));
        RoleWorkflow.touch(role, now);
        return ViewMapper.event(event);
    }

    RoleEvent event(Role role, EventKind kind, Instant now, Map<String, Object> details) {
        return event(role, kind, null, null, now, now, null, details);
    }

    RoleEvent childEvent(Role role, EventKind kind, Interview interview, Assignment assignment, Instant now,
                         Map<String, Object> details) {
        return event(role, kind, interview, assignment, now, now, null, details);
    }

    RoleEvent manualEvent(Role role, EventKind kind, Instant now, Instant occurredAt, String body) {
        return event(role, kind, null, null, now, occurredAt, body, Map.of());
    }

    private RoleEvent event(Role role, EventKind kind, Interview interview, Assignment assignment, Instant now,
                            Instant occurredAt, String body, Map<String, Object> details) {
        RoleEvent event = new RoleEvent();
        event.id = UUID.randomUUID();
        event.role = role;
        event.kind = kind;
        event.interview = interview;
        event.assignment = assignment;
        event.occurredAt = occurredAt;
        event.createdAt = now;
        event.body = body;
        event.details = details;
        return events.save(event);
    }

}
