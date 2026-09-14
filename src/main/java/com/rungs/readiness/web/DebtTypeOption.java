package com.rungs.readiness.web;

import com.rungs.readiness.domain.model.DebtType;
import java.util.List;

/**
 * One choice in the debt row's type {@code <select>}: a {@link DebtType}'s form value and its display label (e.g.
 * {@code CREDIT_CARD} displays as "CREDIT CARD"), so the template has no hard-coded list of its own to keep in sync
 * with {@link DebtType}.
 *
 * @param value the form value, exactly {@link DebtType#name()}
 * @param label the display label: {@code value} with underscores as spaces
 */
record DebtTypeOption(String value, String label) {

    /** The order the form lists debt types in, most common first; must name every {@link DebtType}. */
    private static final List<DebtType> DISPLAY_ORDER = List.of(DebtType.CREDIT_CARD, DebtType.AUTO,
            DebtType.STUDENT_LOAN, DebtType.MEDICAL, DebtType.PERSONAL, DebtType.OTHER);

    /** Returns every {@link DebtType}'s option, in the form's display order. */
    static List<DebtTypeOption> all() {
        return DISPLAY_ORDER.stream()
                .map(type -> new DebtTypeOption(type.name(), type.name().replace('_', ' ')))
                .toList();
    }
}
