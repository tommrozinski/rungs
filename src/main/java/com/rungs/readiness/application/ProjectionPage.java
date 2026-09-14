package com.rungs.readiness.application;

import com.rungs.readiness.application.response.MonthlyPeriodResponse;
import com.rungs.readiness.application.response.ProjectionResponse;
import java.util.Objects;

/**
 * Everything the projection page (030) needs beyond {@link ProjectionResponse}: the month of the first debt
 * retirement in the schedule, a figure the template must not compute itself (AC-G).
 *
 * @param projection the same wire response {@code POST /v1/projections} serializes
 * @param firstRetirementMonth the month of the first debt retirement in the schedule; null if no debt ever retires
 */
public record ProjectionPage(ProjectionResponse projection, Integer firstRetirementMonth) {

    /** Creates a page; throws NullPointerException for a null {@code projection}. */
    public ProjectionPage {
        Objects.requireNonNull(projection, "projection");
    }

    /**
     * Returns the page built from {@code projection}. Throws NullPointerException for a null argument.
     */
    public static ProjectionPage from(ProjectionResponse projection) {
        Objects.requireNonNull(projection, "projection");
        Integer firstRetirementMonth = projection.schedule().stream()
                .filter(period -> !period.retiredDebtIds().isEmpty())
                .map(MonthlyPeriodResponse::month)
                .findFirst()
                .orElse(null);
        return new ProjectionPage(projection, firstRetirementMonth);
    }
}
