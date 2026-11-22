package com.jobtracker.dtos.responses;

import java.util.List;

public record Page<T>(List<T> items, int page, int size, long total) {}
