package com.jobtracker.controllers;

import jakarta.validation.Valid;
import com.jobtracker.dtos.requests.SettingsInput;
import com.jobtracker.dtos.responses.Settings;
import com.jobtracker.services.AppSettingsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
class AppSettingsController {
    private final AppSettingsService settings;

    AppSettingsController(AppSettingsService settings) {
        this.settings = settings;
    }

    @GetMapping
    Settings get() {
        return settings.settings();
    }

    @PutMapping
    Settings update(@Valid @RequestBody SettingsInput input) {
        return settings.update(input);
    }
}
