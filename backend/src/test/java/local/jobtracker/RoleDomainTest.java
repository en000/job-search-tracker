package local.jobtracker;

import java.time.*;
import local.jobtracker.domain.Role;
import org.junit.jupiter.api.Test;
import static local.jobtracker.domain.Codes.*;
import static org.assertj.core.api.Assertions.assertThat;

class RoleDomainTest {
    @Test
    void onlyAcceptsSubstagesInTheirMatchingStage() {
        assertThat(Role.isValidPair(Stage.SAVED, null)).isTrue();
        assertThat(Role.isValidPair(Stage.APPLIED, null)).isTrue();
        assertThat(Role.isValidPair(Stage.IN_PROGRESS, Substage.FINAL_INTERVIEW)).isTrue();
        assertThat(Role.isValidPair(Stage.APPLIED, Substage.FINAL_INTERVIEW)).isFalse();
        assertThat(Role.isValidPair(Stage.COMPLETED, Substage.FINAL_INTERVIEW)).isFalse();
    }

    @Test
    void reopeningRestoresActiveStateAndLeavesFollowUpCleared() {
        Role role = new Role();
        role.stage = Stage.IN_PROGRESS;
        role.substage = Substage.FIRST_INTERVIEW_PASSED;
        role.nextActionText = "Email recruiter";
        role.nextActionDueOn = LocalDate.parse("2026-09-13");
        Instant now = Instant.parse("2026-09-12T00:00:00Z");

        role.complete(Outcome.ACCEPTED, now);
        assertThat(role.isActive()).isFalse();
        assertThat(role.completedAt).isEqualTo(now);
        assertThat(role.nextActionDueOn).isNull();
        role.reopen();

        assertThat(role.stage).isEqualTo(Stage.IN_PROGRESS);
        assertThat(role.substage).isEqualTo(Substage.FIRST_INTERVIEW_PASSED);
        assertThat(role.nextActionText).isNull();
        assertThat(role.previousStage).isNull();
        assertThat(role.outcome).isNull();
        assertThat(role.completedAt).isNull();
    }
}
