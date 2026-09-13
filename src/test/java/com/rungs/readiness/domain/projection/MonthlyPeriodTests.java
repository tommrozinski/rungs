package com.rungs.readiness.domain.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rungs.readiness.domain.model.Money;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MonthlyPeriodTests {

    private static final DebtLine LINE =
            new DebtLine("debt-1", Money.ofDollars(500), Money.ZERO, Money.ofDollars(90), Money.ofDollars(410));

    @Test
    void exposesComponentsAsSupplied() {
        MonthlyPeriod period = new MonthlyPeriod(1, List.of(LINE), List.of("debt-1"));

        assertThat(period.month()).isEqualTo(1);
        assertThat(period.lines()).containsExactly(LINE);
        assertThat(period.retiredDebtIds()).containsExactly("debt-1");
    }

    @Test
    void allowsNoRetiredDebts() {
        MonthlyPeriod period = new MonthlyPeriod(1, List.of(LINE), List.of());

        assertThat(period.retiredDebtIds()).isEmpty();
    }

    @Test
    void copiesTheListsSoLaterMutationOfTheSourceListsIsNotVisible() {
        List<DebtLine> lines = new ArrayList<>(List.of(LINE));
        List<String> retired = new ArrayList<>(List.of("debt-1"));

        MonthlyPeriod period = new MonthlyPeriod(1, lines, retired);
        lines.clear();
        retired.clear();

        assertThat(period.lines()).containsExactly(LINE);
        assertThat(period.retiredDebtIds()).containsExactly("debt-1");
    }

    @Test
    void rejectsUnmodifiableAttemptsOnTheReturnedLists() {
        MonthlyPeriod period = new MonthlyPeriod(1, List.of(LINE), List.of("debt-1"));

        assertThatThrownBy(() -> period.lines().add(LINE)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> period.retiredDebtIds().add("debt-2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejectsAMonthBelowOne(int month) {
        assertThatThrownBy(() -> new MonthlyPeriod(month, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("month must be at least 1");
    }

    @Test
    void rejectsANullLinesList() {
        assertThatThrownBy(() -> new MonthlyPeriod(1, null, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("lines");
    }

    @Test
    void rejectsANullRetiredDebtIdsList() {
        assertThatThrownBy(() -> new MonthlyPeriod(1, List.of(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("retiredDebtIds");
    }
}
