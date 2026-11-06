package com.rungs.readiness.web;

import com.rungs.readiness.domain.model.DebtType;
import java.util.List;
import java.util.Locale;

/**
 * One choice in the debt row's type {@code <select>}: a {@link DebtType}'s form value and its display label (e.g.
 * {@code CREDIT_CARD} displays as "Credit card"), so the template has no hard-coded list of its own to keep in sync
 * with {@link DebtType}.
 *
 * @param value the form value, exactly {@link DebtType#name()}
 * @param label the display label: {@code value} in sentence case, with underscores as spaces
 */
record DebtTypeOption(String value, String label) {

    /** The order the form lists debt types in, most common first; must name every {@link DebtType}. */
    private static final List<DebtType> DISPLAY_ORDER = List.of(DebtType.CREDIT_CARD, DebtType.AUTO,
            DebtType.STUDENT_LOAN, DebtType.MEDICAL, DebtType.PERSONAL, DebtType.OTHER);

    /** Returns every {@link DebtType}'s option, in the form's display order. */
    static List<DebtTypeOption> all() {
        return DISPLAY_ORDER.stream()
                .map(type -> new DebtTypeOption(type.name(), label(type)))
                .toList();
    }

    private static String label(DebtType type) {
        String words = type.name().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
