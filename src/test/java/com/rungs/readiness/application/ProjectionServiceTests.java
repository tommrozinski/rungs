package com.rungs.readiness.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rungs.readiness.application.response.ProjectionResponse;
import com.rungs.readiness.domain.stage.CollegeEvaluator;
import com.rungs.readiness.domain.stage.DebtFreeEvaluator;
import com.rungs.readiness.domain.stage.FullEmergencyFundEvaluator;
import com.rungs.readiness.domain.stage.MortgageEvaluator;
import com.rungs.readiness.domain.stage.RetirementEvaluator;
import com.rungs.readiness.domain.stage.StageLadder;
import com.rungs.readiness.domain.stage.StarterFundEvaluator;
import com.rungs.readiness.domain.stage.WealthBuildingEvaluator;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectionServiceTests {

    private final StageLadder ladder = new StageLadder(List.of(
            new StarterFundEvaluator(),
            new DebtFreeEvaluator(),
            new FullEmergencyFundEvaluator(),
            new RetirementEvaluator(),
            new CollegeEvaluator(),
            new MortgageEvaluator(),
            new WealthBuildingEvaluator()));

    private final ProjectionService service = new ProjectionService(ladder);

    private static SnapshotInput stageTwoSnapshot(List<DebtInput> debts) {
        return new SnapshotInput("5000", "4350", "1000", null, null, null, null, debts, null, null);
    }

    @Test
    void requiresALadderAndAnInput() {
        assertThatThrownBy(() -> new ProjectionService(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.project(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void defaultsAreSnowballThreeHundredSixtyMonthsAndZeroExtra() {
        DebtInput debt = new DebtInput("Card", "CREDIT_CARD", "1000", "12", "100");
        ProjectionInput withDefaults = new ProjectionInput(stageTwoSnapshot(List.of(debt)), null, null, null);
        ProjectionInput explicit =
                new ProjectionInput(stageTwoSnapshot(List.of(debt)), "0", "SNOWBALL", 360);

        assertThat(service.project(withDefaults)).isEqualTo(service.project(explicit));
    }

    @Test
    void monthsToNextStageIsNullWhenTheCurrentStageIsNotTwo() {
        // No debts and no starter fund: stage 1 is the current stage (stage 2 is satisfied with no debts).
        SnapshotInput snapshot = new SnapshotInput(null, null, null, null, null, null, null, List.of(), null, null);
        ProjectionInput input = new ProjectionInput(snapshot, null, null, null);

        ProjectionResponse response = service.project(input);

        assertThat(response.monthsToNextStage()).isNull();
        assertThat(response.monthsToDebtFreedom()).isZero();
        assertThat(response.schedule()).isEmpty();
    }

    @Test
    void monthsToNextStageEqualsMonthsToDebtFreedomAtStageTwo() {
        DebtInput debt = new DebtInput("Card", "CREDIT_CARD", "500", "0", "500");
        ProjectionInput input = new ProjectionInput(stageTwoSnapshot(List.of(debt)), null, null, null);

        ProjectionResponse response = service.project(input);

        assertThat(response.monthsToNextStage()).isEqualTo(response.monthsToDebtFreedom()).isEqualTo(1);
    }

    @Test
    void aShortHorizonTruncatesTheScheduleWithNullMonths() {
        DebtInput debt = new DebtInput("Card", "CREDIT_CARD", "100000", "6", "50");
        ProjectionInput input = new ProjectionInput(stageTwoSnapshot(List.of(debt)), null, null, 3);

        ProjectionResponse response = service.project(input);

        assertThat(response.truncated()).isTrue();
        assertThat(response.monthsToDebtFreedom()).isNull();
        assertThat(response.monthsToNextStage()).isNull();
        assertThat(response.schedule()).hasSize(3);
    }

    @Test
    void aNegativeAmortizingDebtIsUnpayable() {
        // Zero surplus (income exactly covers expenses and the $1 minimum) and a 100% annual rate: the minimum
        // covers only a fraction of the monthly interest, so the balance grows every month with nothing left to
        // apply against it.
        DebtInput debt = new DebtInput("Card", "CREDIT_CARD", "10000", "100", "1");
        SnapshotInput snapshot = new SnapshotInput("1", "0", null, null, null, null, null, List.of(debt), null, null);
        ProjectionInput input = new ProjectionInput(snapshot, null, null, 12);

        ProjectionResponse response = service.project(input);

        assertThat(response.unpayableDebtIds()).containsExactly("debt-1");
    }

    @Test
    void theDebtsFieldCarriesIdLabelTypeAndStartingBalanceInSuppliedOrder() {
        DebtInput debt = new DebtInput("Card", "CREDIT_CARD", "1000", "12", "100");
        ProjectionInput input = new ProjectionInput(stageTwoSnapshot(List.of(debt)), null, null, null);

        ProjectionResponse response = service.project(input);

        assertThat(response.debts()).hasSize(1);
        assertThat(response.debts().getFirst().id()).isEqualTo("debt-1");
        assertThat(response.debts().getFirst().label()).isEqualTo("Card");
        assertThat(response.debts().getFirst().type()).isEqualTo("CREDIT_CARD");
        assertThat(response.debts().getFirst().startingBalance().amount()).isEqualTo("1000.00");
    }

    @Test
    void warningsAreEmpty() {
        ProjectionInput input = new ProjectionInput(stageTwoSnapshot(List.of()), null, null, null);

        assertThat(service.project(input).warnings()).isEmpty();
    }

    @Test
    void projectPageCarriesTheSameResponseAsProjectPlusTheFirstRetirementMonth() {
        DebtInput debt = new DebtInput("Card", "CREDIT_CARD", "500", "0", "500");
        ProjectionInput input = new ProjectionInput(stageTwoSnapshot(List.of(debt)), null, null, null);

        ProjectionPage page = service.projectPage(input);

        assertThat(page.projection()).isEqualTo(service.project(input));
        assertThat(page.firstRetirementMonth()).isEqualTo(1);
    }
}
