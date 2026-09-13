package com.rungs.readiness.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rungs.readiness.application.response.AssessmentResponse;
import com.rungs.readiness.domain.model.Debt;
import com.rungs.readiness.domain.model.DebtType;
import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.HouseholdFlags;
import com.rungs.readiness.domain.model.Money;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 029 REQ-002, REQ-007: the figures and title the assessment page needs beyond the wire response. */
class AssessmentPageTests {

    private static final FinancialSnapshot SNAPSHOT = new FinancialSnapshot(
            Money.ofDollars(4000), Money.ofDollars(3000), Money.ofDollars(500), Money.ofDollars(9000),
            Money.ZERO, BigDecimal.ZERO, 3,
            List.of(new Debt("debt-1", "Card", DebtType.CREDIT_CARD, Money.ofDollars(1200), BigDecimal.TEN,
                    Money.ofDollars(50))),
            null, new HouseholdFlags(true, false, false));

    private static final ReadinessAssessment ASSESSMENT =
            new ReadinessAssessment(3, List.of(), Money.ofDollars(500), List.of());

    @Test
    void carriesTheSameResponseTheApiSerializes() {
        AssessmentPage page = AssessmentPage.from(ASSESSMENT, SNAPSHOT);

        assertThat(page.assessment()).isEqualTo(AssessmentResponse.from(ASSESSMENT));
    }

    @Test
    void totalDebtIsTheSnapshotsTotalDebtBalance() {
        AssessmentPage page = AssessmentPage.from(ASSESSMENT, SNAPSHOT);

        assertThat(page.totalDebt().amount()).isEqualTo(SNAPSHOT.totalDebtBalance().toBareString());
    }

    @Test
    void emergencyFundIsTheSnapshotsFullEmergencyFund() {
        AssessmentPage page = AssessmentPage.from(ASSESSMENT, SNAPSHOT);

        assertThat(page.emergencyFund().amount()).isEqualTo("9000.00");
    }

    @Test
    void surplusPositiveIsTrueOnlyAboveZero() {
        assertThat(AssessmentPage.from(ASSESSMENT, SNAPSHOT).surplusPositive()).isTrue();

        ReadinessAssessment negative = new ReadinessAssessment(3, List.of(), Money.ofDollars(-1), List.of());
        assertThat(AssessmentPage.from(negative, SNAPSHOT).surplusPositive()).isFalse();

        ReadinessAssessment zero = new ReadinessAssessment(3, List.of(), Money.ZERO, List.of());
        assertThat(AssessmentPage.from(zero, SNAPSHOT).surplusPositive()).isFalse();
    }

    @Test
    void documentTitleNamesTheCurrentStageOfSeven() {
        assertThat(AssessmentPage.from(ASSESSMENT, SNAPSHOT).documentTitle()).isEqualTo("Your stage: 3 of 7 · Rungs");

        ReadinessAssessment stageSeven = new ReadinessAssessment(7, List.of(), Money.ZERO, List.of());
        assertThat(AssessmentPage.from(stageSeven, SNAPSHOT).documentTitle()).isEqualTo("Your stage: 7 of 7 · Rungs");
    }

    @Test
    void rejectsNullArguments() {
        assertThatThrownBy(() -> AssessmentPage.from(null, SNAPSHOT)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AssessmentPage.from(ASSESSMENT, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AssessmentPage(null, null, null, false, "t"))
                .isInstanceOf(NullPointerException.class);
    }
}
