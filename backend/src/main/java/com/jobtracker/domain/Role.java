package com.jobtracker.domain;

import com.jobtracker.domain.enums.RoleStatus;
import com.jobtracker.domain.enums.Stage;
import com.jobtracker.domain.enums.Outcome;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;


@Entity
@Table(name = "roles")
public class Role {
    public static final Duration COMPLETION_SUGGESTION_DELAY = Duration.ofDays(14);

    @Id
    public UUID id;

    public UUID getId() {
        return id;
    }
    public String company;
    public String title;
    public String postingUrl;
    public String location;
    public String workArrangement;
    public String salaryText;
    public String notes;
    @Enumerated(EnumType.STRING)
    public RoleStatus status;
    @Enumerated(EnumType.STRING)
    public Stage stage;
    @Enumerated(EnumType.STRING)
    public Outcome outcome;
    public Instant completedAt;
    public String nextActionText;
    public LocalDate nextActionDueOn;
    @Version
    public Long version;
    public Instant createdAt;
    public Instant updatedAt;
    public Instant lastActivityAt;
    public Instant stageChangedAt;
    public boolean completionSuggestionDismissed;
    public Instant completionSuggestionSnoozedUntil;

    public boolean isActive() {
        return status != RoleStatus.COMPLETED;
    }

    public static boolean isValidPair(RoleStatus status, Stage stage) {
        return status != null && status.allowsStage(stage);
    }

    @PrePersist
    @PreUpdate
    public void validateProgress() {
        if (status == null) throw new IllegalStateException("A role must have a status.");
        if (!status.allowsStage(stage)) {
            throw new IllegalStateException("Only an In progress role can have a stage.");
        }
        if (!status.allowsOutcome(outcome)) {
            throw new IllegalStateException("Completed roles require an outcome; other statuses cannot have one.");
        }
    }

    public void recordStageChange(Instant now) {
        stageChangedAt = now;
        completionSuggestionDismissed = false;
        completionSuggestionSnoozedUntil = null;
    }

    public boolean canSuggestCompletion(Instant now) {
        return (status == RoleStatus.APPLIED || status == RoleStatus.IN_PROGRESS)
                && !stageChangedAt.isAfter(now.minus(COMPLETION_SUGGESTION_DELAY))
                && !completionSuggestionDismissed
                && (completionSuggestionSnoozedUntil == null || !completionSuggestionSnoozedUntil.isAfter(now));
    }

    public void complete(Outcome result, Instant now) {
        Objects.requireNonNull(result, "Choose an outcome before completing a role.");
        status = RoleStatus.COMPLETED;
        stage = null;
        outcome = result;
        completedAt = now;
        clearFollowUp();
    }

    public void reopen() {
        status = RoleStatus.SAVED;
        stage = null;
        outcome = null;
        completedAt = null;
    }

    public void clearFollowUp() {
        nextActionText = null;
        nextActionDueOn = null;
    }

}
