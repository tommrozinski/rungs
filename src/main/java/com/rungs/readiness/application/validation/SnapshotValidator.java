package com.rungs.readiness.application.validation;

import com.rungs.readiness.application.ProjectionInput;
import com.rungs.readiness.application.SnapshotInput;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The one application-layer entry point that validates a {@link SnapshotInput} (story 011 review finding): every
 * caller, the API adapter and the form adapter alike, validates through here rather than calling the Jakarta
 * {@link Validator} directly, so nothing can silently run a plain {@code @Valid} and skip the {@link WholeForm}
 * 50-debt rule.
 *
 * <p>Framework-free: it wraps whichever {@code Validator} the caller's Spring context provides, but declares no
 * Spring dependency itself.
 */
public final class SnapshotValidator {

    private static final Comparator<FieldRejection> ORDER =
            Comparator.comparing(FieldRejection::pointer, PropertyPaths.ORDER)
                    .thenComparing(rejection -> rejection.rejection().code());

    private static final String SNAPSHOT_AT_ROOT = "";

    private final Validator validator;

    /** Creates the validator over {@code validator}; throws NullPointerException if it is null. */
    public SnapshotValidator(Validator validator) {
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    /**
     * Validates {@code snapshot} with {@link FieldsThenWholeForm} (REQ-007): every field-only rule, including on
     * each debt and the mortgage, then {@link WholeForm}'s 50-debt rule only if none of them failed. Returns every
     * violation, each with its pointer, in pointer order; empty when the snapshot is valid.
     */
    public List<FieldRejection> validate(SnapshotInput snapshot) {
        return fieldRejections(validator.validate(snapshot, FieldsThenWholeForm.class));
    }

    /**
     * Validates {@code input} with {@link FieldsThenWholeForm} (story 010, 011 REQ-011): the same sequence as
     * {@link #validate(SnapshotInput)}, cascaded into the nested {@code snapshot}, so a violation there is reported
     * at a pointer prefixed with {@code /snapshot} (the property path already includes that component's name).
     * Returns every violation, in pointer order; empty when {@code input} is valid.
     */
    public List<FieldRejection> validate(ProjectionInput input) {
        return fieldRejections(validator.validate(input, FieldsThenWholeForm.class));
    }

    /**
     * Validates only {@link WholeForm}'s 50-debt rule against {@code snapshot} (story 027's debts-table add action):
     * the rule tolerates a missing debt entry or an unparseable balance on its own (it counts neither), so a form
     * that is mid-edit and not yet field-valid can still be checked for the debt count alone, without the {@link
     * FieldsThenWholeForm} sequence's field-only rules hiding it. Returns the {@code TOO_MANY_DEBTS} violation at
     * {@code /debts} when there are, or empty otherwise.
     */
    public List<FieldRejection> validateDebtCount(SnapshotInput snapshot) {
        return fieldRejections(validator.validate(snapshot, WholeForm.class));
    }

    /**
     * Validates one property of {@code bean} (on blur, REQ-008): only that property's own field-only rules, checked
     * with the default group. {@code bean} may be a {@code SnapshotInput}, a {@code DebtInput}, or a
     * {@code MortgageInput}. Returns the first rejection, or empty when the property is valid.
     */
    public <T> Optional<Rejection> validateProperty(T bean, String propertyName) {
        Set<ConstraintViolation<T>> violations = validator.validateProperty(bean, propertyName);
        return violations.stream().findFirst().map(Rejection::of);
    }

    private List<FieldRejection> fieldRejections(Set<? extends ConstraintViolation<?>> violations) {
        List<FieldRejection> fieldRejections = new ArrayList<>();
        for (ConstraintViolation<?> violation : violations) {
            String pointer = PropertyPaths.fromViolation(SNAPSHOT_AT_ROOT, violation.getPropertyPath());
            fieldRejections.add(new FieldRejection(pointer, Rejection.of(violation)));
        }
        fieldRejections.sort(ORDER);
        return List.copyOf(fieldRejections);
    }
}
