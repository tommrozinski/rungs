package com.rungs.readiness.adapter.in.web.view;

import com.rungs.readiness.application.InputText;
import com.rungs.readiness.application.ProjectionPage;
import com.rungs.readiness.application.SnapshotInput;
import com.rungs.readiness.application.response.DebtLineResponse;
import com.rungs.readiness.application.response.MonthlyPeriodResponse;
import com.rungs.readiness.application.response.ProjectionDebtResponse;
import com.rungs.readiness.application.response.ProjectionResponse;
import com.rungs.readiness.application.response.WarningResponse;
import com.rungs.readiness.domain.model.Money;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Builds a {@link ProjectionPageView} from a {@link ProjectionPage} (030), so {@code projection/page.html} only prints. */
final class ProjectionPageViews {

    private static final String DOCUMENT_TITLE = "Your payoff plan · Rungs";

    private static final String DEFAULT_STRATEGY = "SNOWBALL";

    private static final int VISIBLE_MONTHS = 24;

    private static final String DASH = "—";

    private static final String NOT_WITHIN_HORIZON = "Not within 30 years";

    private static final String DEBT_FREE_LABEL = "Debt-free in";

    private static final String TOTAL_INTEREST_LABEL = "Total interest";

    private static final String FIRST_DEBT_LABEL = "First debt paid off";

    private static final String NEUTRAL_TONE = "neutral";

    /** The fixed heading every caution banner shows above its own message (story 012 warnings have none of their own). */
    private static final String CAUTION_HEADING = "Check these numbers";

    private ProjectionPageViews() {
    }

    /**
     * Returns the whole page view for {@code page}. {@code normalizedForHiddenFields} is the {@code forForm()}-
     * normalized snapshot submitted, flattened into the control form's hidden fields; {@code extraMonthlyPayment}
     * and {@code strategy} are the raw submitted control values.
     */
    static ProjectionPageView of(ProjectionPage page, SnapshotInput normalizedForHiddenFields,
            String extraMonthlyPayment, String strategy) {
        return new ProjectionPageView(
                DOCUMENT_TITLE,
                money(extraMonthlyPayment).toBareString(),
                InputText.isBlank(strategy) ? DEFAULT_STRATEGY : strategy,
                HiddenSnapshotFields.of(normalizedForHiddenFields),
                results(page, extraMonthlyPayment));
    }

    /** Returns the results region alone, for the {@code /projection/results} fragment response. */
    static ProjectionResultsView results(ProjectionPage page, String extraMonthlyPayment) {
        ProjectionResponse projection = page.projection();
        return new ProjectionResultsView(
                valueLabel(extraMonthlyPayment),
                warnings(projection.warnings()),
                new StatCardView(DEBT_FREE_LABEL, monthsText(projection.monthsToDebtFreedom()), NEUTRAL_TONE),
                new StatCardView(TOTAL_INTEREST_LABEL, projection.totalInterestPaid().display(), NEUTRAL_TONE),
                new StatCardView(FIRST_DEBT_LABEL, firstDebtPaidOffText(page.firstRetirementMonth()), NEUTRAL_TONE),
                cappedSchedule(projection));
    }

    /** Returns every month's schedule, with no cap, for the {@code /projection/schedule} fragment response. */
    static ScheduleView fullSchedule(ProjectionPage page) {
        ProjectionResponse projection = page.projection();
        List<String> columns = scheduleColumns(projection.debts());
        List<ScheduleRowView> rows = scheduleRows(projection.debts(), projection.schedule());
        return new ScheduleView(columns, rows, false, null, rows.size(), projection.truncated());
    }

    private static ScheduleView cappedSchedule(ProjectionResponse projection) {
        List<String> columns = scheduleColumns(projection.debts());
        List<ScheduleRowView> allRows = scheduleRows(projection.debts(), projection.schedule());
        int total = allRows.size();
        boolean showAll = total > VISIBLE_MONTHS;
        List<ScheduleRowView> visible = showAll ? allRows.subList(0, VISIBLE_MONTHS) : allRows;
        String showAllLabel = showAll ? "Show all " + total + " months" : null;
        return new ScheduleView(columns, visible, showAll, showAllLabel, total, projection.truncated());
    }

    private static List<String> scheduleColumns(List<ProjectionDebtResponse> debts) {
        List<String> columns = new ArrayList<>();
        columns.add("Month");
        for (ProjectionDebtResponse debt : debts) {
            columns.add(debt.label() + " payment");
            columns.add(debt.label() + " balance");
        }
        return columns;
    }

    private static List<ScheduleRowView> scheduleRows(
            List<ProjectionDebtResponse> debts, List<MonthlyPeriodResponse> schedule) {
        List<ScheduleRowView> rows = new ArrayList<>();
        for (MonthlyPeriodResponse period : schedule) {
            Map<String, DebtLineResponse> byDebtId = new HashMap<>();
            for (DebtLineResponse line : period.lines()) {
                byDebtId.put(line.debtId(), line);
            }
            List<String> cells = new ArrayList<>();
            for (ProjectionDebtResponse debt : debts) {
                DebtLineResponse line = byDebtId.get(debt.id());
                cells.add(line == null ? DASH : line.payment().display());
                cells.add(line == null ? DASH : line.closingBalance().display());
            }
            rows.add(new ScheduleRowView("Month " + period.month(), cells, !period.retiredDebtIds().isEmpty()));
        }
        return rows;
    }

    private static List<WarningBannerView> warnings(List<WarningResponse> warnings) {
        return warnings.stream().map(warning -> new WarningBannerView(CAUTION_HEADING, warning.message())).toList();
    }

    private static String monthsText(Integer months) {
        if (months == null) {
            return NOT_WITHIN_HORIZON;
        }
        return months + (months == 1 ? " month" : " months");
    }

    private static String firstDebtPaidOffText(Integer firstRetirementMonth) {
        return firstRetirementMonth == null ? NOT_WITHIN_HORIZON : "Month " + firstRetirementMonth;
    }

    private static String valueLabel(String extraMonthlyPayment) {
        String display = money(extraMonthlyPayment).toDisplayString();
        String trimmed = display.endsWith(".00") ? display.substring(0, display.length() - 3) : display;
        return trimmed + " extra a month";
    }

    private static Money money(String text) {
        return InputText.isBlank(text) ? Money.ZERO : Money.parse(text);
    }
}
