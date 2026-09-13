package com.rungs.readiness.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DebtTypeTests {

    @Test
    void hasExactlyTheSixConsumerDebtTypesInOrderAndNoMortgage() {
        assertThat(DebtType.values()).containsExactly(
                DebtType.STUDENT_LOAN,
                DebtType.AUTO,
                DebtType.CREDIT_CARD,
                DebtType.MEDICAL,
                DebtType.PERSONAL,
                DebtType.OTHER);
        assertThat(DebtType.values()).extracting(Enum::name).doesNotContain("MORTGAGE");
    }
}
