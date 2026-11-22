package com.jobtracker.dtos.responses;

import com.jobtracker.domain.enums.AssignmentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AssignmentDetails(
    UUID id, UUID roleId, String title, AssignmentStatus status, LocalDate receivedOn, LocalDate dueOn,
    String instructions, String instructionsUrl, String workUrl, String notes, String feedback,
    Instant submittedAt, long version, Instant createdAt, Instant updatedAt
) {}
