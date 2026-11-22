package com.jobtracker.dtos.responses;

import java.time.LocalDate;
import java.util.List;

public record Today(
    LocalDate localDate, String timeZone, List<TodayItem> overdue,
    List<TodayItem> dueToday, List<TodayItem> upcoming, List<TodayItem> unscheduled,
    List<CompletionSuggestion> completionSuggestions
) {}
