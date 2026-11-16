package local.jobtracker.dtos;

import java.time.*;
import java.util.*;
import static local.jobtracker.domain.Codes.*;

public final class Responses {
    private Responses() {}

    public record Page<T>(List<T> items, int page, int size, long total) {}
    public record FollowUp(String text, LocalDate dueOn) {}

    public record RoleDetails(
        UUID id, String company, String title, String postingUrl, String location,
        String workArrangement, String salaryText, String notes, Stage stage, Substage substage,
        Stage previousStage, Substage previousSubstage, Outcome outcome, Instant completedAt,
        FollowUp followUp, long version, Instant createdAt, Instant updatedAt, Instant lastActivityAt,
        Instant stageChangedAt, boolean completionSuggestionDismissed, Instant completionSuggestionSnoozedUntil
    ) {}

    public record RoleSummary(
        UUID id, String company, String title, Stage stage, Substage substage,
        Outcome outcome, Instant lastActivityAt, NextItem nextScheduledItem
    ) {}

    public record NextItem(TodayKind kind, UUID childId, String label, LocalDate dueOn, Instant startsAt) {}

    public record ParticipantDetails(UUID id, String name, String jobTitle, String email) {}

    public record InterviewDetails(
        UUID id, UUID roleId, String title, InterviewType type, InterviewStatus status,
        Integer roundNumber, Instant startsAt, Integer durationMinutes, String meetingUrl, String location,
        String prepNotes, String interviewNotes, String feedback, List<ParticipantDetails> participants,
        long version, Instant createdAt, Instant updatedAt
    ) {}

    public record AssignmentDetails(
        UUID id, UUID roleId, String title, AssignmentStatus status, LocalDate receivedOn, LocalDate dueOn,
        String brief, String briefUrl, String workUrl, String submissionUrl, String workNotes, String feedback,
        Instant submittedAt, long version, Instant createdAt, Instant updatedAt
    ) {}

    public record EventDetails(
        UUID id, UUID roleId, UUID interviewId, UUID assignmentId, EventKind kind,
        Instant occurredAt, Instant createdAt, String body, Map<String, Object> details
    ) {}

    public record TodayItem(
        TodayKind kind, UUID roleId, String company, String roleTitle, String label,
        UUID childId, LocalDate dueOn, Instant startsAt, Long roleVersion
    ) {}

    public record Today(
        LocalDate localDate, String timeZone, List<TodayItem> overdue,
        List<TodayItem> dueToday, List<TodayItem> upcoming, List<TodayItem> unscheduled,
        List<CompletionSuggestion> completionSuggestions
    ) {}

    public record CompletionSuggestion(UUID roleId, String company, String roleTitle, long roleVersion) {}

    public record Settings(String timeZone) {}

    public record ExportRole(
        @com.fasterxml.jackson.annotation.JsonUnwrapped RoleDetails role,
        List<InterviewDetails> interviews, List<AssignmentDetails> assignments, List<EventDetails> timeline
    ) {}

    public record Export(int schemaVersion, Instant exportedAt, Settings settings, List<ExportRole> roles) {}
}
