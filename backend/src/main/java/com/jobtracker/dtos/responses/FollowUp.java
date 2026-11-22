package com.jobtracker.dtos.responses;

import java.time.LocalDate;

public record FollowUp(String text, LocalDate dueOn) {}
