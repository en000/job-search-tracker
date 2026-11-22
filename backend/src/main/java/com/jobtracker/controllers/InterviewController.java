package com.jobtracker.controllers;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import com.jobtracker.dtos.requests.*;
import com.jobtracker.dtos.responses.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.jobtracker.services.InterviewService;

@RestController
@RequestMapping("/api/roles/{roleId}/interviews")
class InterviewController {
    private final InterviewService interviews;

    InterviewController(InterviewService interviews) {
        this.interviews = interviews;
    }

    @GetMapping
    List<InterviewDetails> interviews(@PathVariable UUID roleId) {
        return interviews.listInterviews(roleId);
    }

    @PostMapping
    ResponseEntity<InterviewDetails> createInterview(@PathVariable UUID roleId, @Valid @RequestBody InterviewInput input) {
        var interview = interviews.createInterview(roleId, input);
        return ResponseEntity.created(URI.create("/api/roles/" + roleId + "/interviews/" + interview.id())).body(interview);
    }

    @GetMapping("/{interviewId}")
    InterviewDetails interview(@PathVariable UUID roleId, @PathVariable UUID interviewId) {
        return interviews.getInterview(roleId, interviewId);
    }

    @PutMapping("/{interviewId}")
    InterviewDetails updateInterview(@PathVariable UUID roleId, @PathVariable UUID interviewId,
                                     @Valid @RequestBody InterviewInput input) {
        return interviews.updateInterview(roleId, interviewId, input);
    }

}
