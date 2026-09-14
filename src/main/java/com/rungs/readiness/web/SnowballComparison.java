package com.rungs.readiness.web;

import java.time.YearMonth;

/**
 * The debt snowball's mortgage payoff date, and how much sooner it is than the current payment's own payoff date,
 * for stage 6's line and verdict. {@code duration} is null when the current payment has no payoff date to compare
 * against (not covered, or capped at 600 months).
 */
record SnowballComparison(YearMonth date, String duration) {
}
