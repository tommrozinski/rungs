package com.rungs.readiness.adapter.in.web.api;

import com.rungs.readiness.application.validation.PropertyPaths;
import jakarta.validation.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import tools.jackson.core.JacksonException;

/**
 * Builds RFC 6901 JSON Pointers to request fields (story 011) from a validation property path or a Jackson binding
 * path, and orders them for the problem body. Every pointer starts with a prefix: empty for a top-level snapshot, or
 * {@code /snapshot} where the snapshot is nested (REQ-011). The validation-path half of this (shared with the form,
 * story 027) lives in {@link PropertyPaths}; only the Jackson-specific half is local to this adapter.
 */
final class JsonPointers {

    /** See {@link PropertyPaths#ORDER}. */
    static final Comparator<String> ORDER = PropertyPaths.ORDER;

    private static final String SEPARATOR = "/";

    private JsonPointers() {
    }

    /** See {@link PropertyPaths#fromViolation(String, Path)}. */
    static String fromViolation(String prefix, Path path) {
        return PropertyPaths.fromViolation(prefix, path);
    }

    /** Returns the pointer for a Jackson binding {@code path}: property names and array indexes become segments. */
    static String fromJackson(String prefix, List<JacksonException.Reference> path) {
        List<String> segments = new ArrayList<>();
        for (JacksonException.Reference reference : path) {
            if (reference.getPropertyName() != null) {
                segments.add(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                segments.add(Integer.toString(reference.getIndex()));
            }
        }
        StringBuilder pointer = new StringBuilder(prefix);
        for (String segment : segments) {
            pointer.append(SEPARATOR).append(segment.replace("~", "~0").replace("/", "~1"));
        }
        return pointer.toString();
    }
}
