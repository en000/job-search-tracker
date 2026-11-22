package com.jobtracker.dtos.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record FollowUpChange(@NotNull @PositiveOrZero Long expectedVersion, @com.fasterxml.jackson.annotation.JsonProperty(required = true) @Valid FollowUp followUp) {}
