package com.rungs.readiness.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.stage.CollegeEvaluator;
import com.rungs.readiness.domain.stage.DebtFreeEvaluator;
import com.rungs.readiness.domain.stage.FullEmergencyFundEvaluator;
import com.rungs.readiness.domain.stage.MortgageEvaluator;
import com.rungs.readiness.domain.stage.RetirementEvaluator;
import com.rungs.readiness.domain.stage.StageLadder;
import com.rungs.readiness.domain.stage.StarterFundEvaluator;
import com.rungs.readiness.domain.stage.WealthBuildingEvaluator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Story 010 NFR: a 360-month, 10-debt projection completes well under the 250ms p95 budget (PRD NFR-3), measured
 * locally through {@link ProjectionService}. A median of 20 warm runs, not a single sample, keeps the assertion from
 * flaking on one slow JIT-warmup or GC pause; the bound is generous so this fails loudly, not intermittently.
 */
class ProjectionServicePerformanceTests {

    private static final int RUNS = 20;

    private static final long MAX_MEDIAN_MILLIS = 250;

    private final ProjectionService service = new ProjectionService(new StageLadder(List.of(
            new StarterFundEvaluator(),
            new DebtFreeEvaluator(),
            new FullEmergencyFundEvaluator(),
            new RetirementEvaluator(),
            new CollegeEvaluator(),
            new MortgageEvaluator(),
            new WealthBuildingEvaluator())));

    @Test
    void aWarm360MonthTenDebtProjectionHasAMedianUnder250Milliseconds() {
        ProjectionInput input = tenDebtInput();

        // Warm-up: let the JIT compile the hot paths before timing.
        for (int i = 0; i < RUNS; i++) {
            service.project(input);
        }

        long[] millis = new long[RUNS];
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            service.project(input);
            millis[i] = (System.nanoTime() - start) / 1_000_000;
        }

        assertThat(median(millis)).as("median of %s runs: %s", RUNS, Arrays.toString(millis))
                .isLessThan(MAX_MEDIAN_MILLIS);
    }

    private static long median(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        int middle = sorted.length / 2;
        return sorted.length % 2 == 0 ? (sorted[middle - 1] + sorted[middle]) / 2 : sorted[middle];
    }

    private static ProjectionInput tenDebtInput() {
        List<DebtInput> debts = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            debts.add(new DebtInput("Debt " + i, "CREDIT_CARD", "5000", "18", "100"));
        }
        SnapshotInput snapshot =
                new SnapshotInput("10000", "3000", "1000", null, null, null, null, debts, null, null);
        return new ProjectionInput(snapshot, "500", "SNOWBALL", 360);
    }
}
