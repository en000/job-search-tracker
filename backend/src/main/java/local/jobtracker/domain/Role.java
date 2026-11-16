package local.jobtracker.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

import static local.jobtracker.domain.Codes.*;

@Entity
@Table(name = "roles")
public class Role {
    public static final Duration COMPLETION_SUGGESTION_DELAY = Duration.ofDays(14);

    @Id
    public UUID id;
    public UUID workspaceId;
    public String company;
    public String title;
    public String postingUrl;
    public String location;
    public String workArrangement;
    public String salaryText;
    public String notes;
    @Enumerated(EnumType.STRING)
    public Stage stage;
    @Enumerated(EnumType.STRING)
    public Stage previousStage;
    @Enumerated(EnumType.STRING)
    public Substage substage;
    @Enumerated(EnumType.STRING)
    public Substage previousSubstage;
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
        return stage != Stage.COMPLETED;
    }

    public static boolean isValidPair(Stage stage, Substage substage) {
        return substage == null || stage == Stage.IN_PROGRESS;
    }

    public void recordStageChange(Instant now) {
        stageChangedAt = now;
        completionSuggestionDismissed = false;
        completionSuggestionSnoozedUntil = null;
    }

    public boolean canSuggestCompletion(Instant now) {
        return (stage == Stage.APPLIED || stage == Stage.IN_PROGRESS)
                && !stageChangedAt.isAfter(now.minus(COMPLETION_SUGGESTION_DELAY))
                && !completionSuggestionDismissed
                && (completionSuggestionSnoozedUntil == null || !completionSuggestionSnoozedUntil.isAfter(now));
    }

    public void complete(Outcome result, Instant now) {
        previousStage = stage;
        previousSubstage = substage;
        stage = Stage.COMPLETED;
        substage = null;
        outcome = result;
        completedAt = now;
        clearFollowUp();
    }

    public void reopen() {
        stage = previousStage;
        substage = previousSubstage;
        previousStage = null;
        previousSubstage = null;
        outcome = null;
        completedAt = null;
    }

    public void clearFollowUp() {
        nextActionText = null;
        nextActionDueOn = null;
    }

}
