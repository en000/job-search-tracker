package com.jobtracker.dtos.requests;

import com.jobtracker.domain.enums.Outcome;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record Completion(@NotNull @PositiveOrZero Long expectedVersion, @NotNull Outcome outcome) {}
