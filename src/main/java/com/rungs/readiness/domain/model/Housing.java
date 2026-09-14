package com.rungs.readiness.domain.model;

/**
 * How a household pays for its home: renting or paying down a mortgage. Every {@link FinancialSnapshot} has exactly
 * one, never both.
 */
public sealed interface Housing permits Rent, Mortgage {
}
