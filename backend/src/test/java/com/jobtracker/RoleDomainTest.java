package com.jobtracker;

import com.jobtracker.domain.enums.RoleStatus;
import com.jobtracker.domain.enums.Stage;
import com.jobtracker.domain.enums.Outcome;

import java.time.*;
import com.jobtracker.domain.Role;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleDomainTest {
    @Test
    void onlyAcceptsStagesInTheirMatchingStatus() {
        assertThat(Role.isValidPair(null, null)).isFalse();
        assertThat(Role.isValidPair(RoleStatus.SAVED, null)).isTrue();
        assertThat(Role.isValidPair(RoleStatus.APPLIED, null)).isTrue();
        assertThat(Role.isValidPair(RoleStatus.IN_PROGRESS, Stage.FINAL_INTERVIEW)).isTrue();
        assertThat(Role.isValidPair(RoleStatus.APPLIED, Stage.FINAL_INTERVIEW)).isFalse();
        assertThat(Role.isValidPair(RoleStatus.COMPLETED, Stage.FINAL_INTERVIEW)).isFalse();
    }

    @Test
    void progressRequiresDetailsThatBelongToItsStatus() {
        Role role = new Role();
        assertThatThrownBy(role::validateProgress).isInstanceOf(IllegalStateException.class);

        for (RoleStatus status : new RoleStatus[] { RoleStatus.SAVED, RoleStatus.APPLIED }) {
            role.status = status;
            assertThatCode(role::validateProgress).doesNotThrowAnyException();
            role.stage = Stage.FINAL_INTERVIEW;
            assertThatThrownBy(role::validateProgress).isInstanceOf(IllegalStateException.class);
            role.stage = null;
            role.outcome = Outcome.REJECTED;
            assertThatThrownBy(role::validateProgress).isInstanceOf(IllegalStateException.class);
            role.outcome = null;
        }

        role.status = RoleStatus.IN_PROGRESS;
        assertThatCode(role::validateProgress).doesNotThrowAnyException();
        role.stage = Stage.FINAL_INTERVIEW;
        assertThatCode(role::validateProgress).doesNotThrowAnyException();
        role.outcome = Outcome.ACCEPTED;
        assertThatThrownBy(role::validateProgress).isInstanceOf(IllegalStateException.class);

        role.status = RoleStatus.COMPLETED;
        assertThatThrownBy(role::validateProgress).isInstanceOf(IllegalStateException.class);
        role.stage = null;
        assertThatCode(role::validateProgress).doesNotThrowAnyException();
        role.outcome = null;
        assertThatThrownBy(role::validateProgress).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void completingWithoutAnOutcomeLeavesTheRoleUnchanged() {
        Role role = new Role();
        role.status = RoleStatus.IN_PROGRESS;
        role.stage = Stage.FINAL_INTERVIEW;

        assertThatThrownBy(() -> role.complete(null, Instant.parse("2026-11-22T00:00:00Z")))
                .isInstanceOf(NullPointerException.class);
        assertThat(role.status).isEqualTo(RoleStatus.IN_PROGRESS);
        assertThat(role.stage).isEqualTo(Stage.FINAL_INTERVIEW);
        assertThat(role.completedAt).isNull();
    }

    @Test
    void reopeningResetsToSavedAndLeavesFollowUpCleared() {
        Role role = new Role();
        role.status = RoleStatus.IN_PROGRESS;
        role.stage = Stage.FIRST_INTERVIEW_PASSED;
        role.nextActionText = "Email recruiter";
        role.nextActionDueOn = LocalDate.parse("2026-09-13");
        Instant now = Instant.parse("2026-09-12T00:00:00Z");

        role.complete(Outcome.ACCEPTED, now);
        assertThat(role.isActive()).isFalse();
        assertThat(role.completedAt).isEqualTo(now);
        assertThat(role.nextActionDueOn).isNull();
        role.reopen();

        assertThat(role.status).isEqualTo(RoleStatus.SAVED);
        assertThat(role.stage).isNull();
        assertThat(role.nextActionText).isNull();
        assertThat(role.outcome).isNull();
        assertThat(role.completedAt).isNull();
    }
}
