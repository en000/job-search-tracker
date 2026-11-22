package com.jobtracker.dtos.responses;

import java.time.Instant;
import java.util.List;

public record Export(int schemaVersion, Instant exportedAt, Settings settings, List<ExportRole> roles) {}
