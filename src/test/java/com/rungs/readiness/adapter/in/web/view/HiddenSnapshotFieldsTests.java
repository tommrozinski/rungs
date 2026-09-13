package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.application.DebtInput;
import com.rungs.readiness.application.ExampleHouseholds;
import com.rungs.readiness.application.SnapshotInput;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 029 REQ-004: the snapshot as hidden fields feeding a call to action, keyed by {@code SnapshotInput} property paths. */
class HiddenSnapshotFieldsTests {

    @Test
    void includesEveryTopLevelFieldAndFlag() {
        SnapshotInput normalized =
                new SnapshotInput("4000", "3000", "500", "9000", "0", "10", 3, List.of(), null,
                        null).forForm();

        List<HiddenField> fields = HiddenSnapshotFields.of(normalized);

        assertThat(fields).contains(
                new HiddenField("monthlyTakeHomeIncome", "4000"),
                new HiddenField("monthlyEssentialExpenses", "3000"),
                new HiddenField("starterEmergencyFund", "500"),
                new HiddenField("fullEmergencyFund", "9000"),
                new HiddenField("monthlyCollegeContribution", "0"),
                new HiddenField("retirementContributionPercent", "10"),
                new HiddenField("emergencyFundMonths", "3"),
                new HiddenField("householdFlags.rents", "false"),
                new HiddenField("householdFlags.retired", "false"),
                new HiddenField("householdFlags.intendsToFundCollege", "false"),
                new HiddenField("mortgage.balance", "0"),
                new HiddenField("mortgage.annualInterestRate", "0"),
                new HiddenField("mortgage.monthlyPayment", "0"),
                new HiddenField("mortgage.homeValue", "0"));
    }

    @Test
    void includesEveryDebtFieldByIndex() {
        SnapshotInput normalized = new SnapshotInput(null, null, null, null, null, null, null,
                List.of(new DebtInput("Card", "CREDIT_CARD", "1200", "10", "50")), null, null).forForm();

        List<HiddenField> fields = HiddenSnapshotFields.of(normalized);

        assertThat(fields).contains(
                new HiddenField("debts[0].label", "Card"),
                new HiddenField("debts[0].type", "CREDIT_CARD"),
                new HiddenField("debts[0].balance", "1200"),
                new HiddenField("debts[0].annualInterestRate", "10"),
                new HiddenField("debts[0].minimumPayment", "50"));
    }

    @Test
    void reproducesEveryExampleHouseholdWithoutLoss() {
        for (String id : ExampleHouseholds.ids()) {
            SnapshotInput normalized = ExampleHouseholds.byId(id).orElseThrow().forForm();

            List<HiddenField> fields = HiddenSnapshotFields.of(normalized);

            assertThat(fields).extracting(HiddenField::name)
                    .contains("monthlyTakeHomeIncome", "mortgage.balance", "householdFlags.rents");
        }
    }

    @Test
    void trueFlagsRenderAsTrue() {
        SnapshotInput normalized = ExampleHouseholds.byId("deep-in-debt").orElseThrow().forForm();

        List<HiddenField> fields = HiddenSnapshotFields.of(normalized);

        assertThat(fields).contains(new HiddenField("householdFlags.rents", "true"));
    }
}
