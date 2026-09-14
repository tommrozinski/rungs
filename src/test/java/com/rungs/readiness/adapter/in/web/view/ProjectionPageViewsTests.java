package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.application.DebtInput;
import com.rungs.readiness.application.HouseholdFlagsInput;
import com.rungs.readiness.application.MortgageInput;
import com.rungs.readiness.application.ProjectionPage;
import com.rungs.readiness.application.SnapshotInput;
import com.rungs.readiness.application.response.DebtLineResponse;
import com.rungs.readiness.application.response.MoneyResponse;
import com.rungs.readiness.application.response.MonthlyPeriodResponse;
import com.rungs.readiness.application.response.ProjectionDebtResponse;
import com.rungs.readiness.application.response.ProjectionResponse;
import com.rungs.readiness.application.response.WarningResponse;
import com.rungs.readiness.domain.model.Money;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 030: builds the projection page's view model, so the template only prints. */
class ProjectionPageViewsTests {

    private static final SnapshotInput SNAPSHOT = new SnapshotInput(null, null, null, null, null, null, null,
            List.of(), new MortgageInput(null, null, null, null), new HouseholdFlagsInput(null, null, null));

    private static ProjectionDebtResponse debt(String id, String label) {
        return new ProjectionDebtResponse(id, label, "CREDIT_CARD", MoneyResponse.of(Money.ofDollars(100)));
    }

    private static ProjectionResponse response(List<ProjectionDebtResponse> debts,
            List<MonthlyPeriodResponse> schedule, Integer monthsToDebtFreedom, boolean truncated,
            List<WarningResponse> warnings) {
        return new ProjectionResponse(null, monthsToDebtFreedom, MoneyResponse.of(Money.ofDollars(123)), truncated,
                List.of(), debts, schedule, warnings);
    }

    @Test
    void valueLabelIsWholeDollarsWhenTheAmountHasNoCents() {
        ProjectionPageView view = ProjectionPageViews.of(
                ProjectionPage.from(response(List.of(), List.of(), 0, false, List.of())), SNAPSHOT.forForm(),
                "300", null);

        assertThat(view.results().valueLabel()).isEqualTo("$300 extra a month");
    }

    @Test
    void valueLabelKeepsCentsWhenThereAreAny() {
        ProjectionResultsView results = ProjectionPageViews.results(
                ProjectionPage.from(response(List.of(), List.of(), 0, false, List.of())), "300.50");

        assertThat(results.valueLabel()).isEqualTo("$300.50 extra a month");
    }

    @Test
    void valueLabelIsZeroWhenBlankOrNull() {
        ProjectionResultsView blank = ProjectionPageViews.results(
                ProjectionPage.from(response(List.of(), List.of(), 0, false, List.of())), "");
        ProjectionResultsView missing = ProjectionPageViews.results(
                ProjectionPage.from(response(List.of(), List.of(), 0, false, List.of())), null);

        assertThat(blank.valueLabel()).isEqualTo("$0 extra a month");
        assertThat(missing.valueLabel()).isEqualTo("$0 extra a month");
    }

    @Test
    void debtFreeInPluralizesAndHandlesNull() {
        assertThat(debtFreeIn(34)).isEqualTo("34 months");
        assertThat(debtFreeIn(1)).isEqualTo("1 month");
        assertThat(debtFreeIn(0)).isEqualTo("0 months");
        assertThat(debtFreeIn(null)).isEqualTo("Not within 30 years");
    }

    private static String debtFreeIn(Integer months) {
        ProjectionResultsView results = ProjectionPageViews.results(
                ProjectionPage.from(response(List.of(), List.of(), months, false, List.of())), null);
        return results.debtFreeIn().figure();
    }

    @Test
    void totalInterestIsTheResponsesDisplayFigure() {
        ProjectionResultsView results = ProjectionPageViews.results(
                ProjectionPage.from(response(List.of(), List.of(), 0, false, List.of())), null);

        assertThat(results.totalInterest().figure()).isEqualTo("$123.00");
    }

    @Test
    void firstDebtPaidOffNamesTheMonthOrSaysNotWithinTheHorizon() {
        MonthlyPeriodResponse retiresInMonthThree = new MonthlyPeriodResponse(3, List.of(), List.of("debt-1"));
        ProjectionResultsView retires = ProjectionPageViews.results(
                ProjectionPage.from(response(List.of(), List.of(retiresInMonthThree), 3, false, List.of())), null);
        ProjectionResultsView never = ProjectionPageViews.results(
                ProjectionPage.from(response(List.of(), List.of(), null, false, List.of())), null);

        assertThat(retires.firstDebtPaidOff().figure()).isEqualTo("Month 3");
        assertThat(never.firstDebtPaidOff().figure()).isEqualTo("Not within 30 years");
    }

    @Test
    void scheduleColumnsAreMonthThenPaymentAndBalancePerDebtInSuppliedOrder() {
        List<ProjectionDebtResponse> debts = List.of(debt("debt-1", "Credit card"), debt("debt-2", "Car loan"));
        ProjectionResultsView results = ProjectionPageViews.results(
                ProjectionPage.from(response(debts, List.of(), 0, false, List.of())), null);

        assertThat(results.schedule().columns()).containsExactly(
                "Month", "Credit card payment", "Credit card balance", "Car loan payment", "Car loan balance");
    }

    @Test
    void scheduleRowShowsADashForADebtWithNoLineAndTintsWhenADebtRetires() {
        List<ProjectionDebtResponse> debts = List.of(debt("debt-1", "Credit card"), debt("debt-2", "Car loan"));
        DebtLineResponse line = new DebtLineResponse("debt-1", MoneyResponse.of(Money.ofDollars(100)),
                MoneyResponse.of(Money.ZERO), MoneyResponse.of(Money.ofDollars(50)),
                MoneyResponse.of(Money.ofDollars(50)));
        MonthlyPeriodResponse period = new MonthlyPeriodResponse(1, List.of(line), List.of("debt-1"));
        ProjectionResultsView results = ProjectionPageViews.results(
                ProjectionPage.from(response(debts, List.of(period), 1, false, List.of())), null);

        ScheduleRowView row = results.schedule().rows().getFirst();
        assertThat(row.month()).isEqualTo("Month 1");
        assertThat(row.cells()).containsExactly("$50.00", "$50.00", "—", "—");
        assertThat(row.retired()).isTrue();
    }

    @Test
    void scheduleRowIsNotTintedWhenNoDebtRetiresThatMonth() {
        MonthlyPeriodResponse period = new MonthlyPeriodResponse(1, List.of(), List.of());
        ProjectionResultsView results = ProjectionPageViews.results(
                ProjectionPage.from(response(List.of(), List.of(period), 1, false, List.of())), null);

        assertThat(results.schedule().rows().getFirst().retired()).isFalse();
    }

    @Test
    void firstTwentyFourMonthsRenderWithAShowAllButtonBeyondThat() {
        List<MonthlyPeriodResponse> schedule = new ArrayList<>();
        for (int month = 1; month <= 36; month++) {
            schedule.add(new MonthlyPeriodResponse(month, List.of(), List.of()));
        }
        ProjectionResultsView results = ProjectionPageViews.results(
                ProjectionPage.from(response(List.of(), schedule, 36, false, List.of())), null);

        assertThat(results.schedule().rows()).hasSize(24);
        assertThat(results.schedule().rows().getLast().month()).isEqualTo("Month 24");
        assertThat(results.schedule().showAllButton()).isTrue();
        assertThat(results.schedule().showAllLabel()).isEqualTo("Show all 36 months");
        assertThat(results.schedule().totalMonths()).isEqualTo(36);
    }

    @Test
    void noShowAllButtonAtOrBelowTwentyFourMonths() {
        List<MonthlyPeriodResponse> schedule = new ArrayList<>();
        for (int month = 1; month <= 24; month++) {
            schedule.add(new MonthlyPeriodResponse(month, List.of(), List.of()));
        }
        ProjectionResultsView results = ProjectionPageViews.results(
                ProjectionPage.from(response(List.of(), schedule, 24, false, List.of())), null);

        assertThat(results.schedule().rows()).hasSize(24);
        assertThat(results.schedule().showAllButton()).isFalse();
        assertThat(results.schedule().showAllLabel()).isNull();
    }

    @Test
    void fullScheduleReturnsEveryMonthWithNoShowAllButton() {
        List<MonthlyPeriodResponse> schedule = new ArrayList<>();
        for (int month = 1; month <= 36; month++) {
            schedule.add(new MonthlyPeriodResponse(month, List.of(), List.of()));
        }
        ProjectionPage page = ProjectionPage.from(response(List.of(), schedule, 36, false, List.of()));

        ScheduleView full = ProjectionPageViews.fullSchedule(page);

        assertThat(full.rows()).hasSize(36);
        assertThat(full.showAllButton()).isFalse();
        assertThat(full.showAllLabel()).isNull();
        assertThat(full.totalMonths()).isEqualTo(36);
    }

    @Test
    void truncatedCarriesThroughToTheScheduleView() {
        ProjectionResultsView results = ProjectionPageViews.results(
                ProjectionPage.from(response(List.of(), List.of(), null, true, List.of())), null);

        assertThat(results.schedule().truncated()).isTrue();
    }

    @Test
    void warningsMapToBannersWithTheFixedCautionHeading() {
        WarningResponse warning = new WarningResponse("NEGATIVE_AMORTIZATION", "CAUTION", "A message.", List.of());
        ProjectionResultsView results = ProjectionPageViews.results(
                ProjectionPage.from(response(List.of(), List.of(), 0, false, List.of(warning))), null);

        assertThat(results.warnings()).hasSize(1);
        assertThat(results.warnings().getFirst().message()).isEqualTo("A message.");
        assertThat(results.warnings().getFirst().heading()).isNotBlank();
    }

    @Test
    void strategyDefaultsToSnowballAndOtherwisePassesThrough() {
        ProjectionPage page = ProjectionPage.from(response(List.of(), List.of(), 0, false, List.of()));

        assertThat(ProjectionPageViews.of(page, SNAPSHOT.forForm(), null, null).strategy()).isEqualTo("SNOWBALL");
        assertThat(ProjectionPageViews.of(page, SNAPSHOT.forForm(), null, "").strategy()).isEqualTo("SNOWBALL");
        assertThat(ProjectionPageViews.of(page, SNAPSHOT.forForm(), null, "AVALANCHE").strategy())
                .isEqualTo("AVALANCHE");
    }

    @Test
    void extraMonthlyPaymentValueIsTheBareAmount() {
        ProjectionPage page = ProjectionPage.from(response(List.of(), List.of(), 0, false, List.of()));

        assertThat(ProjectionPageViews.of(page, SNAPSHOT.forForm(), "300", null).extraMonthlyPaymentValue())
                .isEqualTo("300.00");
        assertThat(ProjectionPageViews.of(page, SNAPSHOT.forForm(), null, null).extraMonthlyPaymentValue())
                .isEqualTo("0.00");
    }

    @Test
    void hiddenFieldsFlattenTheNormalizedSnapshot() {
        SnapshotInput withDebt = new SnapshotInput("5000", null, null, null, null, null, null,
                List.of(new DebtInput("Card", "CREDIT_CARD", "100", "10", "5")),
                new MortgageInput(null, null, null, null), new HouseholdFlagsInput(null, null, null));
        ProjectionPage page = ProjectionPage.from(response(List.of(), List.of(), 0, false, List.of()));

        ProjectionPageView view = ProjectionPageViews.of(page, withDebt.forForm(), null, null);

        assertThat(view.hiddenFields()).isEqualTo(HiddenSnapshotFields.of(withDebt.forForm()));
    }

    @Test
    void documentTitleIsFixed() {
        ProjectionPage page = ProjectionPage.from(response(List.of(), List.of(), 0, false, List.of()));

        assertThat(ProjectionPageViews.of(page, SNAPSHOT.forForm(), null, null).documentTitle())
                .isEqualTo("Your payoff plan · Rungs");
    }
}
