package com.rungs.readiness.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.model.DebtType;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class DebtTypeOptionTests {

    @Test
    void listsEveryDebtTypeExactlyOnceWithCreditCardFirst() {
        assertThat(DebtTypeOption.all())
                .extracting(DebtTypeOption::value)
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(DebtType.values()).map(DebtType::name).toList())
                .startsWith("CREDIT_CARD");
    }

    @Test
    void labelsReplaceUnderscoresWithSpaces() {
        assertThat(DebtTypeOption.all())
                .contains(new DebtTypeOption("STUDENT_LOAN", "STUDENT LOAN"));
    }
}
