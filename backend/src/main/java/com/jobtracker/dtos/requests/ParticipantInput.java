package com.jobtracker.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ParticipantInput(
    UUID id,
    @NotBlank @Size(max = 200) String name,
    @Size(max = 200) String jobTitle,
    @Email @Size(max = 254) String email
) {}
