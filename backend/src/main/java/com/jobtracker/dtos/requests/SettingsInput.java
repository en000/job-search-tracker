package com.jobtracker.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.jobtracker.domain.enums.Theme;

public record SettingsInput(@NotBlank @Size(max = 100) String timeZone, Theme theme) {}
