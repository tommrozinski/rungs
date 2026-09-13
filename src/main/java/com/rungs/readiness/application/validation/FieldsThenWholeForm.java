package com.rungs.readiness.application.validation;

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;

/**
 * The group to validate a whole submitted snapshot with (story 011 REQ-007): every field-only rule, including those on
 * each debt and the mortgage, then the {@link WholeForm} rules only if none failed.
 *
 * <p>An explicit sequence rather than a {@code @GroupSequence} redefining the snapshot's default group: Hibernate
 * Validator gates a redefined default sequence on the snapshot's own constraints only, so an invalid debt field would
 * not stop the whole-form rule. A single field is still validated with the default group, as in
 * {@code validateProperty(input, "monthlyTakeHomeIncome")} (REQ-008).
 */
@GroupSequence({Default.class, WholeForm.class})
public interface FieldsThenWholeForm {
}
