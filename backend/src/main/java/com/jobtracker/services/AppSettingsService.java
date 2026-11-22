package com.jobtracker.services;

import java.time.ZoneId;
import com.jobtracker.domain.AppSettings;
import com.jobtracker.dtos.responses.Settings;
import com.jobtracker.dtos.requests.SettingsInput;
import com.jobtracker.repository.AppSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppSettingsService {
    private final AppSettingsRepository settings;

    AppSettingsService(AppSettingsRepository settings) {
        this.settings = settings;
    }

    private AppSettings current() {
        return settings.findById(1).orElseThrow(() -> TrackerException.missing("Settings"));
    }

    @Transactional(readOnly = true)
    public Settings settings() {
        AppSettings current = current();
        return new Settings(current.timeZone, current.theme);
    }

    @Transactional
    public Settings update(SettingsInput input) {
        String zone = input.timeZone();
        if (!ZoneId.getAvailableZoneIds().contains(zone)) {
            throw TrackerException.invalid("timeZone", "Choose a valid IANA time zone.");
        }
        AppSettings current = current();
        current.timeZone = zone;
        if (input.theme() != null) current.theme = input.theme();
        return new Settings(current.timeZone, current.theme);
    }
}
