package com.jobtracker.dtos.requests;

import com.jobtracker.domain.enums.AssignmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

public record AssignmentInput(
    @PositiveOrZero Long expectedVersion,
    @NotBlank @Size(max = 200) String title,
    AssignmentStatus status,
    LocalDate receivedOn,
    @NotNull(message = "Enter a due date.") LocalDate dueOn,
    @Size(max = 20000) String instructions,
    @Size(max = 2048) String instructionsUrl,
    @Size(max = 2048) String workUrl,
    @Size(max = 20000) String notes,
    @Size(max = 20000) String feedback,
    Instant submittedAt
) {}
