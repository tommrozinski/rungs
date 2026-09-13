package com.rungs.readiness.domain.model;

/**
 * How seriously a {@link Warning} should be treated. Red is reserved for input validation, so every warning is
 * {@code CAUTION} (012 REQ-006); more values may be added later, and clients treat an unknown severity as caution.
 */
public enum Severity {

    /** A condition worth a caveat, never a failure. */
    CAUTION
}
