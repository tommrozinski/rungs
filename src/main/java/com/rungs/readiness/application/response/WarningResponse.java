package com.rungs.readiness.application.response;

import com.rungs.readiness.application.Warning;

/**
 * One warning of an assessment response; component order is the JSON property order.
 *
 * @param code the machine-readable warning code
 * @param message the user-facing sentence
 */
public record WarningResponse(String code, String message) {

    /** Returns the wire form of {@code warning}. */
    public static WarningResponse from(Warning warning) {
        return new WarningResponse(warning.code(), warning.message());
    }
}
