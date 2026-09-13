package com.rungs.readiness.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * One non-fatal condition reported with an assessment or projection (PRD FR-5), detected by
 * {@link com.rungs.readiness.domain.stage.WarningDetector}.
 *
 * @param code the machine-readable warning code
 * @param severity how seriously the condition should be treated; always {@link Severity#CAUTION} for now
 * @param message the user-facing sentence, complete and ready to display
 * @param debtIds ids of the debts the warning is about, in the order detected; empty when not debt-specific
 */
public record Warning(WarningCode code, Severity severity, String message, List<String> debtIds) {

    /** Copies {@code debtIds}; throws NullPointerException for any null component. */
    public Warning {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(message, "message");
        debtIds = List.copyOf(Objects.requireNonNull(debtIds, "debtIds"));
    }
}
