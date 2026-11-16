package local.jobtracker.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import static local.jobtracker.domain.Codes.*;

public final class Requests {
    private Requests() {}

    public record RoleCreate(
        @NotBlank @Size(max = 200) String company,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2048) String postingUrl,
        @Size(max = 200) String location,
        @Size(max = 200) String workArrangement,
        @Size(max = 200) String salaryText,
        @Size(max = 20000) String notes,
        Stage stage,
        Substage substage
    ) {}

    public record RolePatch(
        @NotNull @PositiveOrZero Long expectedVersion,
        @Size(max = 200) String company,
        @Size(max = 200) String title,
        @Size(max = 2048) String postingUrl,
        @Size(max = 200) String location,
        @Size(max = 200) String workArrangement,
        @Size(max = 200) String salaryText,
        @Size(max = 20000) String notes,
        Stage stage,
        Substage substage
    ) {}

    public record Version(@NotNull @PositiveOrZero Long expectedVersion) {}
    public record Completion(@NotNull @PositiveOrZero Long expectedVersion, @NotNull Outcome outcome) {}
    public enum SuggestionAction { DISMISS, SNOOZE }
    public record CompletionSuggestionChange(
        @NotNull @PositiveOrZero Long expectedVersion, @NotNull SuggestionAction action
    ) {}
    public record FollowUp(@NotBlank @Size(max = 20000) String text, LocalDate dueOn) {}
    public record FollowUpChange(@NotNull @PositiveOrZero Long expectedVersion, @com.fasterxml.jackson.annotation.JsonProperty(required = true) @Valid FollowUp followUp) {}

    public record ParticipantInput(
        UUID id,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 200) String jobTitle,
        @Email @Size(max = 254) String email
    ) {}

    public record InterviewInput(
        @PositiveOrZero Long expectedVersion,
        @NotBlank @Size(max = 200) String title,
        @NotNull InterviewType type,
        InterviewStatus status,
        @Positive Integer roundNumber,
        Instant startsAt,
        @Positive Integer durationMinutes,
        @Size(max = 2048) String meetingUrl,
        @Size(max = 200) String location,
        @Size(max = 20000) String prepNotes,
        @Size(max = 20000) String interviewNotes,
        @Size(max = 20000) String feedback,
        @Size(max = 100) List<@NotNull @Valid ParticipantInput> participants
    ) {}

    public record AssignmentInput(
        @PositiveOrZero Long expectedVersion,
        @NotBlank @Size(max = 200) String title,
        AssignmentStatus status,
        LocalDate receivedOn,
        @NotNull(message = "Enter a due date.") LocalDate dueOn,
        @Size(max = 20000) String brief,
        @Size(max = 2048) String briefUrl,
        @Size(max = 2048) String workUrl,
        @Size(max = 2048) String submissionUrl,
        @Size(max = 20000) String workNotes,
        @Size(max = 20000) String feedback,
        Instant submittedAt
    ) {}

    public record EventInput(
        @NotNull EventKind kind,
        @NotBlank @Size(max = 20000) String body,
        Instant occurredAt
    ) {}

    public record SettingsInput(@NotBlank @Size(max = 100) String timeZone) {}
    public enum RoleSort { recent, nextScheduled }
}
