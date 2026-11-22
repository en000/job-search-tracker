package com.jobtracker.dtos.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CompletionSuggestionChange(
    @NotNull @PositiveOrZero Long expectedVersion, @NotNull SuggestionAction action
) {}
