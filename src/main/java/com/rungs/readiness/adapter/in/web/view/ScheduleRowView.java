package com.rungs.readiness.adapter.in.web.view;

import java.util.List;
import java.util.Objects;

/**
 * One row of the C-10 schedule table, with every figure already formatted by the server.
 *
 * @param month the row header, such as "Month 3"
 * @param cells the formatted cell values, in column order after the month column
 * @param retired whether a debt is retired in this month, which tints the row
 */
public record ScheduleRowView(String month, List<String> cells, boolean retired) {

    /** Copies {@code cells}; null components throw {@link NullPointerException}. */
    public ScheduleRowView {
        Objects.requireNonNull(month, "month");
        cells = List.copyOf(Objects.requireNonNull(cells, "cells"));
    }
}
