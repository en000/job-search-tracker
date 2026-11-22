package com.jobtracker.dtos.requests;

import com.jobtracker.domain.enums.RoleStatus;
import com.jobtracker.domain.enums.Stage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleCreate(
    @NotBlank @Size(max = 200) String company,
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2048) String postingUrl,
    @Size(max = 200) String location,
    @Size(max = 200) String workArrangement,
    @Size(max = 200) String salaryText,
    @Size(max = 20000) String notes,
    RoleStatus status,
    Stage stage
) {}
