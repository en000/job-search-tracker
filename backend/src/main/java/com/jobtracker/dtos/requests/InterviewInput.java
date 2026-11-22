package com.jobtracker.dtos.requests;

import com.jobtracker.domain.enums.InterviewStatus;
import com.jobtracker.domain.enums.InterviewType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

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
