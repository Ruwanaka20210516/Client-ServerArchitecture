package com.smartcampus.mapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small builder that produces a consistent JSON-serialisable error body.
 * Keeping the payload flat-and-friendly (ordered map) means clients always
 * see the same fields regardless of which mapper fired.
 */
public final class ErrorPayload {

    private ErrorPayload() {
    }

    public static Map<String, Object> of(int status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        body.put("timestamp", Instant.now().toString());
        return body;
    }

    public static Map<String, Object> of(int status, String error, String message, Map<String, Object> details) {
        Map<String, Object> body = of(status, error, message);
        if (details != null && !details.isEmpty()) {
            body.put("details", details);
        }
        return body;
    }
}
