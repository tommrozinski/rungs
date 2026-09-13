package com.rungs.readiness.application;

/**
 * Yes-or-no household facts as submitted; a null flag means false.
 *
 * @param rents whether the household rents its home
 * @param retired whether the household is retired
 * @param intendsToFundCollege whether the household intends to fund college
 */
public record HouseholdFlagsInput(Boolean rents, Boolean retired, Boolean intendsToFundCollege) {
}
