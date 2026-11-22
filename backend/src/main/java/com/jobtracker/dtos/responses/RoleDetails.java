package com.jobtracker.dtos.responses;

import com.jobtracker.domain.enums.Outcome;
import com.jobtracker.domain.enums.RoleStatus;
import com.jobtracker.domain.enums.Stage;
import java.time.Instant;
import java.util.UUID;

public record RoleDetails(
    UUID id, String company, String title, String postingUrl, String location,
    String workArrangement, String salaryText, String notes, RoleStatus status, Stage stage,
    Outcome outcome, Instant completedAt,
    FollowUp followUp, long version, Instant createdAt, Instant updatedAt, Instant lastActivityAt,
    Instant stageChangedAt, boolean completionSuggestionDismissed, Instant completionSuggestionSnoozedUntil
) {}
