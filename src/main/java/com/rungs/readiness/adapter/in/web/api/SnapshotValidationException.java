package com.rungs.readiness.adapter.in.web.api;

import com.rungs.readiness.application.validation.FieldRejection;
import java.util.List;
import java.util.Objects;

/**
 * A submitted snapshot failed {@link com.rungs.readiness.application.validation.SnapshotValidator} (story 011);
 * {@link ProblemDetailsAdvice} turns its rejections into the RFC 9457 validation problem body.
 */
final class SnapshotValidationException extends RuntimeException {

    private final transient List<FieldRejection> violations;

    /** Creates the exception over {@code violations}, which must not be empty. */
    SnapshotValidationException(List<FieldRejection> violations) {
        super("Snapshot failed validation with " + Objects.requireNonNull(violations, "violations").size()
                + " violation(s)");
        this.violations = List.copyOf(violations);
    }

    /** Returns every violation, in the order {@code SnapshotValidator} reported them. */
    List<FieldRejection> violations() {
        return violations;
    }
}
