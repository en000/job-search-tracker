package com.jobtracker.dtos.responses;

import com.jobtracker.domain.enums.TodayKind;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TodayItem(
    TodayKind kind, UUID roleId, String company, String roleTitle, String label,
    UUID childId, LocalDate dueOn, Instant startsAt, Long roleVersion
) {}
