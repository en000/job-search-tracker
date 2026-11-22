package com.jobtracker.domain.enums;

public enum RoleStatus {
    SAVED,
    APPLIED,
    IN_PROGRESS,
    COMPLETED;

    public boolean allowsStage(Stage stage) {
        return stage == null || this == IN_PROGRESS;
    }

    public boolean allowsOutcome(Outcome outcome) {
        return this == COMPLETED ? outcome != null : outcome == null;
    }
}
