package com.jobtracker.controllers;

import jakarta.validation.Valid;
import java.util.Map;
import com.jobtracker.dtos.requests.SettingsInput;
import com.jobtracker.dtos.responses.*;
import com.jobtracker.services.TrackerService;
import com.jobtracker.services.AppSettingsService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
class TrackerController {
    private final TrackerService tracker;
    private final AppSettingsService settings;

    TrackerController(TrackerService tracker, AppSettingsService settings) {
        this.tracker = tracker;
        this.settings = settings;
    }

    @GetMapping("/health")
    Map<String, String> health() {
        settings.settings();
        return Map.of("status", "UP");
    }

    @GetMapping("/today")
    Today today() {
        return tracker.today();
    }

    @GetMapping("/export")
    ResponseEntity<Export> export() {
        Export snapshot = tracker.export();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=job-search-tracker.json")
                .body(snapshot);
    }
}
