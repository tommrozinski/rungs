package com.rungs.readiness.application.validation;

/**
 * The wording shared by the API's problem {@code detail} and the form's C-09 error summary (story 011 REQ-006, story
 * 027): how many fields need attention, singular or plural.
 */
public final class FieldSummary {

    private FieldSummary() {
    }

    /** Returns "1 field needs attention." or "N fields need attention.". */
    public static String detail(int fieldCount) {
        return fieldCount == 1 ? "1 field needs attention." : fieldCount + " fields need attention.";
    }
}
