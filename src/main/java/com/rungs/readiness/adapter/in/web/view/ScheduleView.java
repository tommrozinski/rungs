package com.rungs.readiness.adapter.in.web.view;

import java.util.List;

/**
 * The C-10 schedule table for the projection page (030 REQ-005, REQ-006): the header row, the rows to render now,
 * and whether more months exist beyond them.
 *
 * @param columns the header row: "Month", then "{label} payment" and "{label} balance" for each debt in supplied
 *     order
 * @param rows the rows to render; either the first 24 months or every month
 * @param showAllButton whether a "Show all N months" button belongs under the table
 * @param showAllLabel the button's own label, e.g. "Show all 36 months"; null when {@code showAllButton} is false
 * @param totalMonths the schedule's total month count
 * @param truncated whether the plan doesn't finish within the horizon
 */
public record ScheduleView(
        List<String> columns,
        List<ScheduleRowView> rows,
        boolean showAllButton,
        String showAllLabel,
        int totalMonths,
        boolean truncated) {
}
