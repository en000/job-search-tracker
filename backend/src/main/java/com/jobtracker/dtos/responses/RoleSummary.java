package com.jobtracker.dtos.responses;

import com.jobtracker.domain.enums.Outcome;
import com.jobtracker.domain.enums.RoleStatus;
import com.jobtracker.domain.enums.Stage;
import java.time.Instant;
import java.util.UUID;

public record RoleSummary(
    UUID id, String company, String title, RoleStatus status, Stage stage,
    Outcome outcome, Instant lastActivityAt, NextItem nextScheduledItem
) {}
