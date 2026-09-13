package com.rungs.readiness.application.response;

import com.rungs.readiness.domain.model.Warning;
import java.util.List;

/**
 * One warning of an assessment or projection response (story 012); component order is the JSON property order.
 *
 * @param code the machine-readable warning code, its enum name
 * @param severity how seriously the condition should be treated, its enum name
 * @param message the user-facing sentence
 * @param debtIds ids of the debts the warning is about, in order; empty when not debt-specific
 */
public record WarningResponse(String code, String severity, String message, List<String> debtIds) {

    /** Copies {@code debtIds}; throws NullPointerException for a null list or element. */
    public WarningResponse {
        debtIds = List.copyOf(debtIds);
    }

    /** Returns the wire form of {@code warning}. */
    public static WarningResponse from(Warning warning) {
        return new WarningResponse(
                warning.code().name(), warning.severity().name(), warning.message(), warning.debtIds());
    }
}
