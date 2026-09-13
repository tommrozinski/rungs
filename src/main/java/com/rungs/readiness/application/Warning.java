package com.rungs.readiness.application;

/**
 * A non-fatal condition reported with an assessment. Placeholder until story 012 defines the codes.
 *
 * @param code the machine-readable warning code
 * @param message the user-facing sentence
 */
public record Warning(String code, String message) {
}
