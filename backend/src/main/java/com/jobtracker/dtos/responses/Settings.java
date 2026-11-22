package com.jobtracker.dtos.responses;

import com.jobtracker.domain.enums.Theme;

public record Settings(String timeZone, Theme theme) {}
