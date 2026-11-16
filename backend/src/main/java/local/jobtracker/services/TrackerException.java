package local.jobtracker.services;

import java.util.Map;

public class TrackerException extends RuntimeException {
    public enum Category { INVALID, MISSING, CONFLICT }

    public final Category category;
    public final String code;
    public final Map<String, String> fieldErrors;

    private TrackerException(Category category, String code, String detail, Map<String, String> fieldErrors) {
        super(detail);
        this.category = category;
        this.code = code;
        this.fieldErrors = fieldErrors;
    }

    public static TrackerException invalid(String field, String detail) {
        return new TrackerException(Category.INVALID, "invalid_input", detail, Map.of(field, detail));
    }

    public static TrackerException missing(String resource) {
        return new TrackerException(Category.MISSING, "not_found", resource + " was not found.", Map.of());
    }

    public static TrackerException conflict(String code, String detail) {
        return new TrackerException(Category.CONFLICT, code, detail, Map.of());
    }

    public static void checkVersion(Long expected, Long actual) {
        if (expected == null) throw invalid("expectedVersion", "An expected version is required.");
        if (!actual.equals(expected)) {
            throw conflict("stale_version", "This record changed since you opened it. Review the latest version before applying your edits.");
        }
    }
}
