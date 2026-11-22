package com.jobtracker.services;

import java.net.URI;
import java.time.*;
import java.time.temporal.ChronoUnit;

final class Inputs {
    private Inputs() {}

    static String optional(String value) {
        if (value == null) return null;
        if (value.indexOf(0) >= 0) throw TrackerException.invalid("body", "Text cannot contain a null character.");
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static String required(String value, String field) {
        String result = optional(value);
        if (result == null) throw TrackerException.invalid(field, "Enter a value.");
        return result;
    }

    static String link(String value, String field) {
        String result = optional(value);
        if (result == null) return null;
        try {
            URI uri = URI.create(result);
            if (uri.getHost() != null && ("https".equalsIgnoreCase(uri.getScheme()) ||
                    "http".equalsIgnoreCase(uri.getScheme()))) return result;
        } catch (IllegalArgumentException ignored) {
            // Report a field error for malformed and non-HTTP links alike.
        }
        throw TrackerException.invalid(field, "Enter an absolute HTTP or HTTPS URL.");
    }

    static Instant timestamp(Instant value) {
        return value == null ? null : value.truncatedTo(ChronoUnit.MICROS);
    }

    static Instant now(Clock clock) {
        return timestamp(clock.instant());
    }
}
