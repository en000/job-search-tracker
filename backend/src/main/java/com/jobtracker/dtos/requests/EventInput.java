package com.jobtracker.dtos.requests;

import com.jobtracker.domain.enums.EventKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record EventInput(
    @NotNull EventKind kind,
    @NotBlank @Size(max = 20000) String body,
    Instant occurredAt
) {}
