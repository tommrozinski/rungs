package com.rungs.readiness.adapter.in.web.api;

import com.rungs.readiness.application.AssessmentService;
import com.rungs.readiness.application.SnapshotInput;
import com.rungs.readiness.application.response.AssessmentResponse;
import com.rungs.readiness.application.validation.FieldsThenWholeForm;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /v1/assessments} (PRD FR-1): assesses a financial snapshot and returns the full stage ladder. The body is
 * validated against the application input model's rules before it is assessed; this controller defines none, and
 * {@link ProblemDetailsAdvice} writes every rejection.
 */
@RestController
public class AssessmentController {

    private final AssessmentService assessmentService;

    /** Creates the controller over {@code assessmentService}. */
    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = Objects.requireNonNull(assessmentService, "assessmentService");
    }

    /** Returns the assessment of the submitted snapshot, once it has passed field and then whole-form validation. */
    @PostMapping(
            path = "/v1/assessments",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public AssessmentResponse assess(@Validated(FieldsThenWholeForm.class) @RequestBody SnapshotInput snapshot) {
        return AssessmentResponse.from(assessmentService.assess(snapshot));
    }
}
