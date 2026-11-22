package com.jobtracker.dtos.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record Version(@NotNull @PositiveOrZero Long expectedVersion) {}
