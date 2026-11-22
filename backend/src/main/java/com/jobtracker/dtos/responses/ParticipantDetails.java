package com.jobtracker.dtos.responses;

import java.util.UUID;

public record ParticipantDetails(UUID id, String name, String jobTitle, String email) {}
