package com.rungs.readiness.web;

import static com.rungs.readiness.domain.model.FinancialSnapshotBuilder.aSnapshot;
import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.model.DebtType;
import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.FinancialSnapshotBuilder;
import com.rungs.readiness.domain.model.HouseholdFlags;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Mortgage;
import com.rungs.readiness.domain.projection.PayoffScheduleCalculator;
import java.math.BigDecimal;
import com.rungs.readiness.domain.stage.CollegeEvaluator;
import com.rungs.readiness.domain.stage.DebtFreeEvaluator;
import com.rungs.readiness.domain.stage.FullEmergencyFundEvaluator;
import com.rungs.readiness.domain.stage.LadderResult;
import com.rungs.readiness.domain.stage.MortgageEvaluator;
import com.rungs.readiness.domain.stage.RetirementEvaluator;
import com.rungs.readiness.domain.stage.StageLadder;
import com.rungs.readiness.domain.stage.StarterFundEvaluator;
import com.rungs.readiness.domain.stage.WealthBuildingEvaluator;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The result page's copy rules (design.md 038 section 4), one test per row of the table plus edge cases. */
class ResultViewsTests {

    private final StageLadder ladder = new StageLadder(List.of(
            new StarterFundEvaluator(),
            new DebtFreeEvaluator(),
            new FullEmergencyFundEvaluator(),
            new RetirementEvaluator(),
            new CollegeEvaluator(),
            new MortgageEvaluator(),
            new WealthBuildingEvaluator()));

    private StageView stage(ResultView view, int stage) {
        return view.stages().get(stage - 1);
    }

    private ResultView build(FinancialSnapshot snapshot) {
        LadderResult result = ladder.assess(snapshot);
        return ResultViews.build(result, snapshot, PayoffScheduleCalculator.schedule(snapshot));
    }

    // Stage 1: starter fund

    @Test
    void stage1DoneShowsTheFundAgainstTheTarget() {
        FinancialSnapshot snapshot = aSnapshot().withStarterEmergencyFund(Money.ofDollars(1_000)).build();

        StageView view = stage(build(snapshot), 1);

        assertThat(view.line()).isEqualTo("$1,000 of $1,000.");
        assertThat(view.chipLabel()).isEqualTo("Done");
        assertThat(view.showBar()).isFalse();
    }

    @Test
    void stage1InProgressShowsTheFundAndTheBar() {
        FinancialSnapshot snapshot = aSnapshot().withStarterEmergencyFund(Money.ofDollars(500)).build();

        StageView view = stage(build(snapshot), 1);

        assertThat(view.line()).isEqualTo("$500 of $1,000.");
        assertThat(view.chipLabel()).isEqualTo("In progress");
        assertThat(view.showBar()).isTrue();
        assertThat(view.barWidthPercent()).isEqualTo(50);
    }

    @Test
    void stage1NotStartedShowsNoBar() {
        FinancialSnapshot snapshot = aSnapshot().withStarterEmergencyFund(Money.ZERO).build();

        StageView view = stage(build(snapshot), 1);

        assertThat(view.line()).isEqualTo("$0 of $1,000.");
        assertThat(view.chipLabel()).isEqualTo("Not started");
        assertThat(view.showBar()).isFalse();
    }

    @Test
    void stage1KeepsCentsWhenNotAWholeDollarAmount() {
        FinancialSnapshot snapshot = aSnapshot().withStarterEmergencyFund(Money.ofCents(50_050)).build();

        StageView view = stage(build(snapshot), 1);

        assertThat(view.line()).isEqualTo("$500.50 of $1,000.");
    }

    // Stage 2: debt free

    @Test
    void stage2DoneShowsNoDebts() {
        FinancialSnapshot snapshot = aSnapshot().build();

        StageView view = stage(build(snapshot), 2);

        assertThat(view.line()).isEqualTo("No debts besides the house.");
    }

    @Test
    void stage2InProgressSpellsOutAPluralDebtCount() {
        FinancialSnapshot snapshot = aSnapshot()
                .withStarterEmergencyFund(Money.ofDollars(1_000))
                .withDebt("Store card", DebtType.CREDIT_CARD, Money.ofDollars(840), "26.99", Money.ofDollars(35))
                .withDebt("Car loan", DebtType.AUTO, Money.ofDollars(9_140), "6.4", Money.ofDollars(312))
                .withDebt("Student loan", DebtType.STUDENT_LOAN, Money.ofDollars(2_500), "4.5", Money.ofDollars(110))
                .build();

        StageView view = stage(build(snapshot), 2);

        assertThat(view.line()).isEqualTo("Three debts remain, totalling <b>$12,480</b>.");
    }

    @Test
    void stage2InProgressUsesSingularWordingForOneDebt() {
        FinancialSnapshot snapshot = aSnapshot()
                .withDebt("Store card", DebtType.CREDIT_CARD, Money.ofDollars(500), "26.99", Money.ofDollars(35))
                .build();

        StageView view = stage(build(snapshot), 2);

        assertThat(view.line()).isEqualTo("One debt remains, totalling <b>$500</b>.");
    }

    @Test
    void stage2InProgressUsesDigitsFallbackAboveTenDebts() {
        FinancialSnapshotBuilder builder = aSnapshot().withStarterEmergencyFund(Money.ofDollars(1_000));
        for (int i = 1; i <= 11; i++) {
            builder.withDebt("Debt " + i, DebtType.CREDIT_CARD, Money.ofDollars(100), "5", Money.ofDollars(10));
        }
        FinancialSnapshot snapshot = builder.build();

        StageView view = stage(build(snapshot), 2);

        assertThat(view.line()).isEqualTo("11 debts remain, totalling <b>$1,100</b>.");
    }

    // Stage 3: full emergency fund

    @Test
    void stage3DoneShowsTheFundAgainstAThreeMonthTarget() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyEssentialExpenses(Money.ofDollars(3_500))
                .withFullEmergencyFund(Money.ofDollars(10_500))
                .build();

        StageView view = stage(build(snapshot), 3);

        assertThat(view.line()).isEqualTo("$10,500 of $10,500.");
        assertThat(view.chipLabel()).isEqualTo("Done");
    }

    @Test
    void stage3InProgressShowsTheBar() {
        FinancialSnapshot snapshot = aSnapshot()
                .withStarterEmergencyFund(Money.ofDollars(1_000))
                .withMonthlyEssentialExpenses(Money.ofDollars(3_500))
                .withFullEmergencyFund(Money.ofDollars(5_000))
                .build();

        StageView view = stage(build(snapshot), 3);

        assertThat(view.line()).isEqualTo("$5,000 of $10,500.");
        assertThat(view.showBar()).isTrue();
        assertThat(view.barWidthPercent()).isEqualTo(47);
    }

    @Test
    void stage3NotStartedShowsNoBar() {
        FinancialSnapshot snapshot = aSnapshot().withMonthlyEssentialExpenses(Money.ofDollars(3_500)).build();

        StageView view = stage(build(snapshot), 3);

        assertThat(view.line()).isEqualTo("$0 of $10,500.");
        assertThat(view.showBar()).isFalse();
    }

    // Stage 4: retirement

    @Test
    void stage4DoneShowsThePercentOnly() {
        FinancialSnapshot snapshot = aSnapshot().withRetirementContributionPercent("15").build();

        StageView view = stage(build(snapshot), 4);

        assertThat(view.line()).isEqualTo("You're at 15%.");
    }

    @Test
    void stage4InProgressSpellsOutTheRemainingPoints() {
        FinancialSnapshot snapshot = aSnapshot().withRetirementContributionPercent("6").build();

        StageView view = stage(build(snapshot), 4);

        assertThat(view.line()).isEqualTo("You're at 6%. Nine points to go.");
        assertThat(view.showBar()).isTrue();
        assertThat(view.barWidthPercent()).isEqualTo(40);
    }

    @Test
    void stage4NotStartedUsesDigitsWhenRemainingPointsExceedTen() {
        FinancialSnapshot snapshot = aSnapshot().withRetirementContributionPercent("0").build();

        StageView view = stage(build(snapshot), 4);

        assertThat(view.line()).isEqualTo("You're at 0%. 15 points to go.");
        assertThat(view.chipLabel()).isEqualTo("Not started");
    }

    @Test
    void stage4SkippedForARetiredHousehold() {
        FinancialSnapshot snapshot = aSnapshot()
                .withHouseholdFlags(new HouseholdFlags(true, false))
                .build();

        StageView view = stage(build(snapshot), 4);

        assertThat(view.line()).isEqualTo("You told us you're retired.");
        assertThat(view.chipLabel()).isEqualTo("Skipped");
    }

    // Stage 5: college

    @Test
    void stage5DoneWhenContributingToCollege() {
        FinancialSnapshot snapshot = aSnapshot()
                .withHouseholdFlags(new HouseholdFlags(false, true))
                .withMonthlyCollegeContribution(Money.ofDollars(200))
                .build();

        StageView view = stage(build(snapshot), 5);

        assertThat(view.line()).isEqualTo("You're saving for college.");
    }

    @Test
    void stage5NotStartedWhenPlanningButNotYetContributing() {
        FinancialSnapshot snapshot = aSnapshot()
                .withHouseholdFlags(new HouseholdFlags(false, true))
                .build();

        StageView view = stage(build(snapshot), 5);

        assertThat(view.line()).isEqualTo("You plan to fund college.");
    }

    @Test
    void stage5SkippedWithNoCollegePlans() {
        FinancialSnapshot snapshot = aSnapshot()
                .withHouseholdFlags(new HouseholdFlags(false, false))
                .build();

        StageView view = stage(build(snapshot), 5);

        assertThat(view.line()).isEqualTo("You told us you're not funding college.");
    }

    @Test
    void stage5SkippedForARetiredHouseholdThatPlannedToFundCollege() {
        FinancialSnapshot snapshot = aSnapshot()
                .withHouseholdFlags(new HouseholdFlags(true, true))
                .build();

        StageView view = stage(build(snapshot), 5);

        assertThat(view.line()).isEqualTo("You told us you're retired.");
    }

    // Stage 6: mortgage

    @Test
    void stage6DoneWithAPaidOffMortgage() {
        Mortgage paidOff = new Mortgage(Money.ZERO, BigDecimal.ZERO, Money.ZERO);
        FinancialSnapshot snapshot = aSnapshot().withHousing(paidOff).build();

        StageView view = stage(build(snapshot), 6);

        assertThat(view.line()).isEqualTo("Your house is paid off.");
    }

    @Test
    void stage6SkippedForARenter() {
        FinancialSnapshot snapshot = aSnapshot().build();

        StageView view = stage(build(snapshot), 6);

        assertThat(view.line()).isEqualTo("You rent, so there's no mortgage to pay down.");
    }

    // Stage 7: wealth building

    @Test
    void stage7DoneWhenEveryEarlierStageIsDoneOrSkipped() {
        FinancialSnapshot snapshot = aSnapshot()
                .withStarterEmergencyFund(Money.ofDollars(1_000))
                .withRetirementContributionPercent("15")
                .build();

        ResultView result = build(snapshot);

        assertThat(stage(result, 7).line()).isEqualTo("Every earlier stage is done or skipped.");
        assertThat(result.eyebrow()).isEqualTo("Every stage that applies to you is done");
    }

    @Test
    void stage7NotStartedWhileEarlierStagesAreOutstanding() {
        FinancialSnapshot snapshot = aSnapshot().build();

        StageView view = stage(build(snapshot), 7);

        assertThat(view.line()).isEqualTo("Opens once stages 1 through 6 are done or skipped.");
    }

    @Test
    void stage7ShowsTheNotStartedPresentationWhileSomeButNotAllEarlierStagesAreDone() {
        // Stage 1, 2, 5, and 6 are satisfied or skipped by default; stages 3 and 4 are not, so
        // WealthBuildingEvaluator reports IN_PROGRESS. The result page still shows stage 7 as Not
        // started (design.md 038 section 4): a plain disc and the chip-waiting chip, never active.
        FinancialSnapshot snapshot = aSnapshot().withStarterEmergencyFund(Money.ofDollars(1_000)).build();

        StageView view = stage(build(snapshot), 7);

        assertThat(view.chipLabel()).isEqualTo("Not started");
        assertThat(view.chipClass()).isEqualTo("chip-waiting");
        assertThat(view.discClass()).isNull();
        assertThat(view.line()).isEqualTo("Opens once stages 1 through 6 are done or skipped.");
    }

    // Eyebrow, stats, and current-stage verdict

    @Test
    void eyebrowNamesTheCurrentStageWhenNotAllDone() {
        FinancialSnapshot snapshot = aSnapshot().build();

        ResultView result = build(snapshot);

        assertThat(result.eyebrow()).isEqualTo("You're on stage 1 of 7");
    }

    @Test
    void stagesCompleteCountsOnlySatisfiedStages() {
        FinancialSnapshot snapshot = aSnapshot()
                .withStarterEmergencyFund(Money.ofDollars(1_000))
                .withMonthlyEssentialExpenses(Money.ofDollars(1_000))
                .withDebt("Store card", DebtType.CREDIT_CARD, Money.ofDollars(500), "10", Money.ofDollars(20))
                .build();

        ResultView result = build(snapshot);

        // Only stage 1 is satisfied; stages 5 and 6 are skipped (no college plans, renter), which does not count.
        assertThat(result.stagesComplete()).isEqualTo("1 of 7");
    }

    @Test
    void monthlySurplusIsPositiveWhenAboveZero() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(6_200))
                .withMonthlyEssentialExpenses(Money.ofDollars(3_500))
                .build();

        ResultView result = build(snapshot);

        assertThat(result.surplusPositive()).isTrue();
        assertThat(result.monthlySurplus()).isEqualTo("$2,700");
    }

    @Test
    void currentStageOneVerdict() {
        FinancialSnapshot snapshot = aSnapshot().withStarterEmergencyFund(Money.ofDollars(400)).build();

        ResultView result = build(snapshot);

        assertThat(result.verdictLine()).isEqualTo("$600 more and your starter fund is done.");
        assertThat(result.verdictSub()).isEqualTo("Save $1,000 before anything else.");
    }

    @Test
    void currentStageTwoVerdictShowsTheSnowballsDebtFreeMonth() {
        FinancialSnapshot snapshot = aSnapshot()
                .withStarterEmergencyFund(Money.ofDollars(1_000))
                .withMonthlyTakeHomeIncome(Money.ofDollars(6_200))
                .withMonthlyEssentialExpenses(Money.ofDollars(3_500))
                .withDebt("Store card", DebtType.CREDIT_CARD, Money.ofDollars(840), "26.99", Money.ofDollars(35))
                .withDebt("Car loan", DebtType.AUTO, Money.ofDollars(9_140), "6.4", Money.ofDollars(312))
                .withDebt("Student loan", DebtType.STUDENT_LOAN, Money.ofDollars(2_500), "4.5", Money.ofDollars(110))
                .build();

        ResultView result = build(snapshot);

        assertThat(result.verdictLine()).isEqualTo("$12,480 stands between you and debt freedom.");
        assertThat(result.verdictSub()).isEqualTo(
                "With the debt snowball you're free of everything but the house by May 2024, about 5 months from now.");
    }

    @Test
    void currentStageTwoVerdictUsesTheSingularMonthWhenTheSnowballClearsTheDebtsThisMonth() {
        FinancialSnapshot snapshot = aSnapshot()
                .withStarterEmergencyFund(Money.ofDollars(1_000))
                .withMonthlyTakeHomeIncome(Money.ofDollars(5_000))
                .withDebt("Store card", DebtType.CREDIT_CARD, Money.ofDollars(500), "10", Money.ofDollars(0))
                .build();

        ResultView result = build(snapshot);

        assertThat(result.verdictSub()).isEqualTo(
                "With the debt snowball you're free of everything but the house by January 2024, about 1 month from now.");
    }

    @Test
    void currentStageTwoVerdictUsesTheGiveUpSentenceWhenTheSnowballNeverClearsTheDebts() {
        FinancialSnapshot snapshot = aSnapshot()
                .withStarterEmergencyFund(Money.ofDollars(1_000))
                .withMonthlyTakeHomeIncome(Money.ofDollars(3_000))
                .withMonthlyEssentialExpenses(Money.ofDollars(3_000))
                .withDebt("Store card", DebtType.CREDIT_CARD, Money.ofDollars(500), "10", Money.ofDollars(0))
                .build();

        ResultView result = build(snapshot);

        assertThat(result.verdictSub()).isEqualTo("At these payments your debts won't be paid off within 50 years.");
    }

    @Test
    void currentStageThreeVerdict() {
        FinancialSnapshot snapshot = aSnapshot()
                .withStarterEmergencyFund(Money.ofDollars(1_000))
                .withMonthlyTakeHomeIncome(Money.ofDollars(6_200))
                .withMonthlyEssentialExpenses(Money.ofDollars(3_500))
                .withFullEmergencyFund(Money.ofDollars(0))
                .build();

        ResultView result = build(snapshot);

        assertThat(result.verdictLine()).isEqualTo("$10,500 more and three months of expenses are covered.");
        assertThat(result.verdictSub())
                .isEqualTo("At your current surplus of $2,700 a month, that's about 4 months of work.");
    }

    @Test
    void currentStageFourVerdict() {
        FinancialSnapshot snapshot = aSnapshot()
                .withStarterEmergencyFund(Money.ofDollars(1_000))
                .withMonthlyEssentialExpenses(Money.ZERO)
                .withRetirementContributionPercent("6")
                .build();

        ResultView result = build(snapshot);

        assertThat(result.verdictLine()).isEqualTo("Raise retirement saving to 15%.");
        assertThat(result.verdictSub()).isEqualTo("You're at 6%.");
    }

    @Test
    void currentStageFiveVerdict() {
        FinancialSnapshot snapshot = aSnapshot()
                .withStarterEmergencyFund(Money.ofDollars(1_000))
                .withMonthlyEssentialExpenses(Money.ZERO)
                .withRetirementContributionPercent("15")
                .withHouseholdFlags(new HouseholdFlags(false, true))
                .build();

        ResultView result = build(snapshot);

        assertThat(result.verdictLine()).isEqualTo("Start saving for college.");
        assertThat(result.verdictSub()).isEqualTo("Set aside something every month.");
    }

    @Test
    void currentStageSixVerdictShowsTheSnowballsEarlierPayoffDate() {
        FinancialSnapshot snapshot = aSnapshot()
                .withStarterEmergencyFund(Money.ofDollars(1_000))
                .withMonthlyTakeHomeIncome(Money.ofDollars(2_000))
                .withMonthlyEssentialExpenses(Money.ZERO)
                .withFullEmergencyFund(Money.ofDollars(3_600))
                .withRetirementContributionPercent("15")
                .withHouseholdFlags(new HouseholdFlags(false, false))
                .withHousing(new Mortgage(
                        Money.ofDollars(100_000), new BigDecimal("6.125"), Money.ofDollars(1_200)))
                .build();

        ResultView result = build(snapshot);

        assertThat(result.verdictLine()).isEqualTo("Pay off the house.");
        assertThat(result.verdictSub()).isEqualTo(
                "With the debt snowball it's paid off in October 2028, 4 years 4 months sooner than your payment alone.");
        assertThat(stage(result, 6).line()).isEqualTo(
                "At your current payment it's paid off in February 2033. With the debt snowball it's paid off in "
                        + "October 2028, 4 years 4 months sooner.");
    }

    @Test
    void currentStageSixVerdictFallsBackToTheEvaluatorsSentenceWhenThePaymentDoesNotCoverInterest() {
        FinancialSnapshot snapshot = aSnapshot()
                .withStarterEmergencyFund(Money.ofDollars(1_000))
                .withMonthlyEssentialExpenses(Money.ZERO)
                .withFullEmergencyFund(Money.ofDollars(3_600))
                .withRetirementContributionPercent("15")
                .withHouseholdFlags(new HouseholdFlags(false, false))
                .withHousing(new Mortgage(
                        Money.ofDollars(100_000), new BigDecimal("6.125"), Money.ofDollars(400)))
                .build();

        ResultView result = build(snapshot);

        assertThat(result.verdictSub())
                .isEqualTo("Your $400.00 payment doesn't cover the $510.42 monthly interest.");
    }

    @Test
    void currentStageSevenVerdictIsAlwaysTheAllDoneCase() {
        // Stage 7 can only ever be the current stage once stages 1-6 are all satisfied or skipped, and at that
        // point the wealth-building evaluator itself is always satisfied (never in progress or not started) —
        // so the stage-7 verdict and the "all done" eyebrow always appear together.
        FinancialSnapshot snapshot = aSnapshot()
                .withStarterEmergencyFund(Money.ofDollars(1_000))
                .withRetirementContributionPercent("15")
                .build();

        ResultView result = build(snapshot);

        assertThat(result.eyebrow()).isEqualTo("Every stage that applies to you is done");
        assertThat(result.verdictLine()).isEqualTo("Build wealth and give.");
        assertThat(result.verdictSub()).isEqualTo("Every earlier stage is done or skipped.");
    }
}
