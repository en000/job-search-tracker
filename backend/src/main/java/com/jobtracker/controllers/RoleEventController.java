package com.jobtracker.controllers;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import com.jobtracker.dtos.requests.*;
import com.jobtracker.dtos.responses.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.*;
import com.jobtracker.services.RoleEventService;

@RestController
@RequestMapping("/api/roles/{roleId}/timeline")
class RoleEventController {
    private final RoleEventService events;

    RoleEventController(RoleEventService events) {
        this.events = events;
    }

    @GetMapping
    Page<EventDetails> timeline(@PathVariable UUID roleId,
            @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return events.timeline(roleId, page, size);
    }

    @GetMapping("/{eventId}")
    EventDetails timelineEvent(@PathVariable UUID roleId, @PathVariable UUID eventId) {
        return events.timelineEvent(roleId, eventId);
    }

    @PostMapping
    ResponseEntity<EventDetails> log(@PathVariable UUID roleId, @Valid @RequestBody EventInput request) {
        var event = events.log(roleId, request);
        return ResponseEntity.created(URI.create("/api/roles/" + roleId + "/timeline/" + event.id())).body(event);
    }
}
