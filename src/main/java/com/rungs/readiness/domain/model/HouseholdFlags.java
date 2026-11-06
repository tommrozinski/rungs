package com.rungs.readiness.domain.model;

/**
 * Yes-or-no facts about a household that decide whether some stages apply.
 *
 * @param retired whether the household is retired
 * @param intendsToFundCollege whether the household intends to fund college for dependents
 */
public record HouseholdFlags(boolean retired, boolean intendsToFundCollege) {
}
