package com.rungs.readiness.adapter.in.web.view;

import java.util.List;

/**
 * Everything {@code assessment/page.html} needs to render S-02 (029 REQ-001, REQ-002): the document title, the
 * verdict, the three stat cards, any warnings, the seven-stage ladder, and the hidden snapshot fields that feed the
 * current row's call to action.
 *
 * @param documentTitle the document title, naming the current stage
 * @param verdict the C-11 verdict block
 * @param surplus the monthly surplus stat card
 * @param totalDebt the total debt stat card
 * @param emergencyFund the emergency fund stat card
 * @param warnings the C-09 caution banners, empty when there are none
 * @param ladder all seven stage rows in order
 * @param hiddenFields the snapshot, flattened for the ladder's current-row call to action
 */
public record AssessmentPageView(
        String documentTitle,
        VerdictView verdict,
        StatCardView surplus,
        StatCardView totalDebt,
        StatCardView emergencyFund,
        List<WarningBannerView> warnings,
        List<StageRowView> ladder,
        List<HiddenField> hiddenFields) {
}
