package com.rungs.readiness.adapter.in.web.view;

import com.rungs.readiness.application.validation.FieldRejection;
import com.rungs.readiness.application.validation.FieldSummary;
import com.rungs.readiness.application.validation.Rejection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns the pointer-keyed violations {@code SnapshotValidator} reports into what the form templates need (story 027
 * REQ-002, REQ-006): each field's error looked up by its form field name (the same names used for
 * {@code @ModelAttribute} binding, such as {@code debts[2].balance} or {@code mortgage.balance}), the whole-form
 * 50-debt violation for the debts table, and the C-09 summary text, worded exactly as the API's (story 011).
 */
final class FormErrors {

    /** The pointer a whole-form violation (story 011 REQ-003) is reported at: the debts list itself. */
    private static final String DEBTS_POINTER = "/debts";

    private FormErrors() {
    }

    /** Returns "N field(s) need attention.", or empty when there are no violations. */
    static Optional<String> summary(List<FieldRejection> violations) {
        return violations.isEmpty() ? Optional.empty() : Optional.of(FieldSummary.detail(violations.size()));
    }

    /**
     * Returns every per-field violation, keyed by its form field name; the whole-form violation at {@value
     * #DEBTS_POINTER} is excluded, since it belongs to the table as a whole, not a single field ({@link #forTable}).
     */
    static Map<String, Rejection> byName(List<FieldRejection> violations) {
        Map<String, Rejection> byName = new LinkedHashMap<>();
        for (FieldRejection violation : violations) {
            if (!DEBTS_POINTER.equals(violation.pointer())) {
                byName.put(nameFor(violation.pointer()), violation.rejection());
            }
        }
        return byName;
    }

    /** Returns the whole-form 50-debt violation, for the debts table, when present. */
    static Optional<Rejection> forTable(List<FieldRejection> violations) {
        return violations.stream().filter(violation -> DEBTS_POINTER.equals(violation.pointer()))
                .map(FieldRejection::rejection).findFirst();
    }

    /**
     * Returns the form field name for {@code pointer}: a list index becomes a bracket, e.g. {@code /debts/2/balance}
     * becomes {@code debts[2].balance}; every other segment becomes a dotted property, e.g. {@code /mortgage/balance}
     * becomes {@code mortgage.balance}. The input models nest no deeper than this, so a fixed rule is enough.
     */
    private static String nameFor(String pointer) {
        String[] segments = pointer.substring(1).split("/");
        StringBuilder name = new StringBuilder(segments[0]);
        for (int index = 1; index < segments.length; index++) {
            String segment = segments[index];
            if (isDigits(segment)) {
                name.append('[').append(segment).append(']');
            } else {
                name.append('.').append(segment);
            }
        }
        return name.toString();
    }

    private static boolean isDigits(String segment) {
        return !segment.isEmpty() && segment.chars().allMatch(Character::isDigit);
    }
}
