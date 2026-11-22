package com.jobtracker.dtos.responses;

import com.jobtracker.domain.enums.EventKind;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EventDetails(
    UUID id, UUID roleId, UUID interviewId, UUID assignmentId, EventKind kind,
    Instant occurredAt, Instant createdAt, String body, Map<String, Object> details
) {}
