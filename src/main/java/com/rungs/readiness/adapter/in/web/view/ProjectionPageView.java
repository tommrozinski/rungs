package com.rungs.readiness.adapter.in.web.view;

import java.util.List;

/**
 * Everything {@code projection/page.html} needs to render S-03 (030 REQ-001, REQ-002): the document title, the
 * control card's current values, the hidden snapshot fields, and the results region.
 *
 * @param documentTitle the document title
 * @param extraMonthlyPaymentValue the range input's current value, a bare decimal such as "300.00"
 * @param strategy the selected strategy, "SNOWBALL" or "AVALANCHE"
 * @param hiddenFields the snapshot, flattened for the control form
 * @param results the results region
 */
public record ProjectionPageView(
        String documentTitle,
        String extraMonthlyPaymentValue,
        String strategy,
        List<HiddenField> hiddenFields,
        ProjectionResultsView results) {
}
