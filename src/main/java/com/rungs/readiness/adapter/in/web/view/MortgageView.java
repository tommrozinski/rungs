package com.rungs.readiness.adapter.in.web.view;

/**
 * The mortgage section's four fields plus whether it is shown (027 REQ-002: shown when the household does not
 * rent). The server always renders the correct visibility, so the page works even if the mortgage-toggle script
 * fails (REQ-011).
 */
public record MortgageView(FieldView balance, FieldView rate, FieldView monthlyPayment, FieldView homeValue,
        boolean visible) {
}
