package com.jobtracker.dtos.responses;

import java.util.List;

public record ExportRole(
    @com.fasterxml.jackson.annotation.JsonUnwrapped RoleDetails role,
    List<InterviewDetails> interviews, List<AssignmentDetails> assignments, List<EventDetails> timeline
) {}
