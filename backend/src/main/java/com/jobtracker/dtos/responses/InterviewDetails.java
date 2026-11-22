package com.jobtracker.dtos.responses;

import com.jobtracker.domain.enums.InterviewStatus;
import com.jobtracker.domain.enums.InterviewType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InterviewDetails(
    UUID id, UUID roleId, String title, InterviewType type, InterviewStatus status,
    Integer roundNumber, Instant startsAt, Integer durationMinutes, String meetingUrl, String location,
    String prepNotes, String interviewNotes, String feedback, List<ParticipantDetails> participants,
    long version, Instant createdAt, Instant updatedAt
) {}
