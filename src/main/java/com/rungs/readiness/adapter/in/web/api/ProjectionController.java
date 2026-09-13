package com.rungs.readiness.adapter.in.web.api;

import com.rungs.readiness.application.ProjectionInput;
import com.rungs.readiness.application.ProjectionService;
import com.rungs.readiness.application.response.ProjectionResponse;
import com.rungs.readiness.application.validation.FieldRejection;
import com.rungs.readiness.application.validation.SnapshotValidator;
import java.util.List;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /v1/projections} (PRD FR-2, story 010): runs the payoff simulation over a submitted snapshot and
 * options. The body is validated by {@link SnapshotValidator}, the one entry point for validating a snapshot,
 * rather than by a {@code @Valid} or {@code @Validated} annotation on this controller; {@link ProblemDetailsAdvice}
 * writes every rejection.
 */
@RestController
public class ProjectionController {

    private final ProjectionService projectionService;

    private final SnapshotValidator snapshotValidator;

    /** Creates the controller over {@code projectionService} and {@code snapshotValidator}. */
    public ProjectionController(ProjectionService projectionService, SnapshotValidator snapshotValidator) {
        this.projectionService = Objects.requireNonNull(projectionService, "projectionService");
        this.snapshotValidator = Objects.requireNonNull(snapshotValidator, "snapshotValidator");
    }

    /** Returns the payoff projection for the submitted request, once it has passed field and whole-form validation. */
    @PostMapping(
            path = "/v1/projections",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ProjectionResponse project(@RequestBody ProjectionInput input) {
        List<FieldRejection> violations = snapshotValidator.validate(input);
        if (!violations.isEmpty()) {
            throw new SnapshotValidationException(violations);
        }
        return projectionService.project(input);
    }
}
