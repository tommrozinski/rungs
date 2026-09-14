package com.rungs.readiness.adapter.in.web.view;

import java.util.List;

/**
 * The projection page's results region (030 REQ-004, REQ-005, REQ-006): the control card's value label, any
 * warnings, the three outcome stat cards, and the schedule.
 *
 * @param valueLabel "$300 extra a month" (or "$0 extra a month"), swapped into the control card out of band on
 *     every response
 * @param warnings the C-09 caution banners, empty when there are none
 * @param debtFreeIn the "Debt-free in" stat card
 * @param totalInterest the "Total interest" stat card
 * @param firstDebtPaidOff the "First debt paid off" stat card
 * @param schedule the schedule table
 */
public record ProjectionResultsView(
        String valueLabel,
        List<WarningBannerView> warnings,
        StatCardView debtFreeIn,
        StatCardView totalInterest,
        StatCardView firstDebtPaidOff,
        ScheduleView schedule) {
}
