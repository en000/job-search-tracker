package com.jobtracker.controllers;

import java.util.List;
import java.util.UUID;
import com.jobtracker.dtos.responses.ParticipantDetails;
import com.jobtracker.services.ParticipantService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roles/{roleId}/interviews/{interviewId}/participants")
class ParticipantController {
    private final ParticipantService participants;

    ParticipantController(ParticipantService participants) {
        this.participants = participants;
    }

    @GetMapping
    List<ParticipantDetails> list(@PathVariable UUID roleId, @PathVariable UUID interviewId) {
        return participants.list(roleId, interviewId);
    }
}
