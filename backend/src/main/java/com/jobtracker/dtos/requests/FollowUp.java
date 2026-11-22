package com.jobtracker.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record FollowUp(@NotBlank @Size(max = 20000) String text, LocalDate dueOn) {}
