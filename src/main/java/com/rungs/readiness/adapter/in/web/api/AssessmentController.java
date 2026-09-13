package com.rungs.readiness.adapter.in.web.api;

import com.rungs.readiness.application.AssessmentService;
import com.rungs.readiness.application.SnapshotInput;
import com.rungs.readiness.application.response.AssessmentResponse;
import com.rungs.readiness.application.validation.FieldRejection;
import com.rungs.readiness.application.validation.SnapshotValidator;
import java.util.List;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /v1/assessments} (PRD FR-1): assesses a financial snapshot and returns the full stage ladder. The body
 * is validated by {@link SnapshotValidator}, the one entry point for validating a snapshot, rather than by a
 * {@code @Valid} or {@code @Validated} annotation on this controller; {@link ProblemDetailsAdvice} writes every
 * rejection.
 */
@RestController
public class AssessmentController {

    private final AssessmentService assessmentService;

    private final SnapshotValidator snapshotValidator;

    /** Creates the controller over {@code assessmentService} and {@code snapshotValidator}. */
    public AssessmentController(AssessmentService assessmentService, SnapshotValidator snapshotValidator) {
        this.assessmentService = Objects.requireNonNull(assessmentService, "assessmentService");
        this.snapshotValidator = Objects.requireNonNull(snapshotValidator, "snapshotValidator");
    }

    /** Returns the assessment of the submitted snapshot, once it has passed field and then whole-form validation. */
    @PostMapping(
            path = "/v1/assessments",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public AssessmentResponse assess(@RequestBody SnapshotInput snapshot) {
        List<FieldRejection> violations = snapshotValidator.validate(snapshot);
        if (!violations.isEmpty()) {
            throw new SnapshotValidationException(violations);
        }
        return AssessmentResponse.from(assessmentService.assess(snapshot));
    }
}
