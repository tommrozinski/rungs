package com.rungs.readiness.adapter.in.web.view;

/**
 * Turns a form field name (a {@code SnapshotInput} property path, such as {@code debts[2].balance} or
 * {@code mortgage.balance}) into a stable HTML element id. htmx 2 restores focus to an element with the same id
 * after an on-blur swap (027 REQ-005), so the id a full render gives a field must match the id its own swap gives it.
 */
final class FieldIds {

    private FieldIds() {
    }

    /** Returns the element id for {@code name}, e.g. {@code debts[2].balance} becomes {@code debts-2-balance}. */
    static String of(String name) {
        return name.replace("[", "-").replace("]", "").replace(".", "-");
    }
}
