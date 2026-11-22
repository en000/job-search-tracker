package com.jobtracker.dtos.responses;

import com.jobtracker.domain.enums.TodayKind;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record NextItem(TodayKind kind, UUID childId, String label, LocalDate dueOn, Instant startsAt) {}
