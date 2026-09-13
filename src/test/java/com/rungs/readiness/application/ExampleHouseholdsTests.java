package com.rungs.readiness.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.application.validation.FieldsThenWholeForm;
import com.rungs.readiness.domain.stage.CollegeEvaluator;
import com.rungs.readiness.domain.stage.DebtFreeEvaluator;
import com.rungs.readiness.domain.stage.FullEmergencyFundEvaluator;
import com.rungs.readiness.domain.stage.MortgageEvaluator;
import com.rungs.readiness.domain.stage.RetirementEvaluator;
import com.rungs.readiness.domain.stage.StageLadder;
import com.rungs.readiness.domain.stage.StarterFundEvaluator;
import com.rungs.readiness.domain.stage.WealthBuildingEvaluator;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** The fixed example household presets (design §3): their ids, values, and stages. */
class ExampleHouseholdsTests {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    private final AssessmentService service = new AssessmentService(new StageLadder(List.of(
            new StarterFundEvaluator(),
            new DebtFreeEvaluator(),
            new FullEmergencyFundEvaluator(),
            new RetirementEvaluator(),
            new CollegeEvaluator(),
            new MortgageEvaluator(),
            new WealthBuildingEvaluator())));

    @AfterAll
    static void close() {
        FACTORY.close();
    }

    @Test
    void idsAreTheThreePresetsInOrder() {
        assertThat(ExampleHouseholds.ids()).containsExactly("deep-in-debt", "mid-journey", "nearly-done");
    }

    @ParameterizedTest
    @ValueSource(strings = {"deep-in-debt", "mid-journey", "nearly-done"})
    void eachPresetPassesFieldsThenWholeFormValidation(String id) {
        SnapshotInput input = ExampleHouseholds.byId(id).orElseThrow();

        assertThat(VALIDATOR.validate(input, FieldsThenWholeForm.class)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"deep-in-debt, 1", "mid-journey, 4", "nearly-done, 6"})
    void eachPresetLandsOnItsStatedStage(String id, int expectedStage) {
        SnapshotInput input = ExampleHouseholds.byId(id).orElseThrow();

        assertThat(service.assess(input).currentStage()).isEqualTo(expectedStage);
    }

    @Test
    void byIdIsEmptyForNullOrUnknown() {
        assertThat(ExampleHouseholds.byId(null)).isEmpty();
        assertThat(ExampleHouseholds.byId("no-such-id")).isEmpty();
    }

    @Test
    void deepInDebtHoldsTheDesignValues() {
        SnapshotInput input = ExampleHouseholds.byId("deep-in-debt").orElseThrow();

        assertThat(input.monthlyTakeHomeIncome()).isEqualTo("4200");
        assertThat(input.monthlyEssentialExpenses()).isEqualTo("3100");
        assertThat(input.starterEmergencyFund()).isEqualTo("400");
        assertThat(input.fullEmergencyFund()).isEqualTo("400");
        assertThat(input.monthlyCollegeContribution()).isEqualTo("0");
        assertThat(input.retirementContributionPercent()).isEqualTo("0");
        assertThat(input.emergencyFundMonths()).isEqualTo(3);
        assertThat(input.mortgage()).isNull();
        assertThat(input.householdFlags()).isEqualTo(new HouseholdFlagsInput(true, false, false));
        assertThat(input.debts()).containsExactly(
                new DebtInput("Store card", "CREDIT_CARD", "1850", "24.99", "55"),
                new DebtInput("Car loan", "AUTO", "9400", "6.9", "285"),
                new DebtInput("Student loan", "STUDENT_LOAN", "21300", "5.5", "240"));
    }

    @Test
    void midJourneyHoldsTheDesignValues() {
        SnapshotInput input = ExampleHouseholds.byId("mid-journey").orElseThrow();

        assertThat(input.monthlyTakeHomeIncome()).isEqualTo("6800");
        assertThat(input.monthlyEssentialExpenses()).isEqualTo("4200");
        assertThat(input.starterEmergencyFund()).isEqualTo("1000");
        assertThat(input.fullEmergencyFund()).isEqualTo("12600");
        assertThat(input.monthlyCollegeContribution()).isEqualTo("0");
        assertThat(input.retirementContributionPercent()).isEqualTo("8.5");
        assertThat(input.emergencyFundMonths()).isEqualTo(3);
        assertThat(input.debts()).isEmpty();
        assertThat(input.mortgage()).isEqualTo(new MortgageInput("186000", "6.25", "1450", "310000"));
        assertThat(input.householdFlags()).isEqualTo(new HouseholdFlagsInput(false, false, true));
    }

    @Test
    void nearlyDoneHoldsTheDesignValues() {
        SnapshotInput input = ExampleHouseholds.byId("nearly-done").orElseThrow();

        assertThat(input.monthlyTakeHomeIncome()).isEqualTo("9500");
        assertThat(input.monthlyEssentialExpenses()).isEqualTo("5000");
        assertThat(input.starterEmergencyFund()).isEqualTo("1000");
        assertThat(input.fullEmergencyFund()).isEqualTo("30000");
        assertThat(input.monthlyCollegeContribution()).isEqualTo("250");
        assertThat(input.retirementContributionPercent()).isEqualTo("15");
        assertThat(input.emergencyFundMonths()).isEqualTo(6);
        assertThat(input.debts()).isEmpty();
        assertThat(input.mortgage()).isEqualTo(new MortgageInput("42000", "3.75", "900", "420000"));
        assertThat(input.householdFlags()).isEqualTo(new HouseholdFlagsInput(false, false, true));
    }
}
