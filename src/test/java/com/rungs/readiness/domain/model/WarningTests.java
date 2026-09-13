package com.rungs.readiness.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WarningTests {

    @Test
    void debtIdsIsAnUnmodifiableCopy() {
        List<String> debtIds = new ArrayList<>(List.of("debt-1"));
        Warning warning = new Warning(WarningCode.NEGATIVE_AMORTIZATION, Severity.CAUTION, "A message.", debtIds);
        debtIds.add("debt-2");

        assertThat(warning.debtIds()).containsExactly("debt-1");
        assertThatThrownBy(() -> warning.debtIds().add("debt-3")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsANullComponent() {
        assertThatThrownBy(() -> new Warning(null, Severity.CAUTION, "A message.", List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Warning(WarningCode.EXPENSES_EXCEED_INCOME, null, "A message.", List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Warning(WarningCode.EXPENSES_EXCEED_INCOME, Severity.CAUTION, null, List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Warning(WarningCode.EXPENSES_EXCEED_INCOME, Severity.CAUTION, "A message.", null))
                .isInstanceOf(NullPointerException.class);
    }
}
