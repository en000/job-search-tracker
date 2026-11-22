package com.jobtracker.dtos.responses;

import java.util.UUID;

public record CompletionSuggestion(UUID roleId, String company, String roleTitle, long roleVersion) {}
