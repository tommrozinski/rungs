package com.rungs.readiness.application.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.application.ReadinessAssessment;
import com.rungs.readiness.application.Warning;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.stage.StageEvaluation;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AssessmentResponseTests {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "0         | 0.00         | $0.00",
        "123456    | 1234.56      | $1,234.56",
        "-5000     | -50.00       | -$50.00",
        "100000005 | 1000000.05   | $1,000,000.05"
    })
    void moneyIsItsBareAmountAndDisplayString(long cents, String amount, String display) {
        assertThat(MoneyResponse.of(Money.ofCents(cents))).isEqualTo(new MoneyResponse(amount, display));
    }

    @Test
    void mapsEveryStageStatusWithPlainPercentAndNullsKept() {
        ReadinessAssessment assessment = new ReadinessAssessment(1,
                List.of(
                        StageEvaluation.inProgress(1, new BigDecimal("50.0"), Money.ofDollars(500), "Short."),
                        StageEvaluation.satisfied(2),
                        StageEvaluation.notStarted(3, null, "Nothing saved."),
                        StageEvaluation.inProgress(4, new BigDecimal("66.6"), null, "Contributing."),
                        StageEvaluation.skipped(5, "No college."),
                        StageEvaluation.skipped(6, "Rents."),
                        StageEvaluation.notStarted(7, Money.ofCents(1), "Earlier stages.")),
                Money.ofDollars(-50),
                List.of(new Warning("CODE", "A message.")));

        assertThat(AssessmentResponse.from(assessment)).isEqualTo(new AssessmentResponse(1,
                List.of(
                        new StageResponse(1, "IN_PROGRESS", "50.0", new MoneyResponse("500.00", "$500.00"), "Short.",
                                null),
                        new StageResponse(2, "SATISFIED", "100.0", null, null, null),
                        new StageResponse(3, "NOT_STARTED", "0.0", null, "Nothing saved.", null),
                        new StageResponse(4, "IN_PROGRESS", "66.6", null, "Contributing.", null),
                        new StageResponse(5, "SKIPPED", "0.0", null, null, "No college."),
                        new StageResponse(6, "SKIPPED", "0.0", null, null, "Rents."),
                        new StageResponse(7, "NOT_STARTED", "0.0", new MoneyResponse("0.01", "$0.01"),
                                "Earlier stages.", null)),
                new MoneyResponse("-50.00", "-$50.00"),
                List.of(new WarningResponse("CODE", "A message."))));
    }

    @Test
    void responseListsAreUnmodifiableCopies() {
        List<StageResponse> stages = new ArrayList<>();
        List<WarningResponse> warnings = new ArrayList<>();
        AssessmentResponse response = new AssessmentResponse(7, stages, new MoneyResponse("0.00", "$0.00"), warnings);
        stages.add(new StageResponse(1, "SATISFIED", "100.0", null, null, null));
        warnings.add(new WarningResponse("CODE", "A message."));

        assertThat(response.stages()).isEmpty();
        assertThat(response.warnings()).isEmpty();
    }
}
