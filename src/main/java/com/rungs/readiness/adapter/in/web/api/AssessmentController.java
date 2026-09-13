package com.rungs.readiness.adapter.in.web.api;

import com.rungs.readiness.application.AssessmentService;
import com.rungs.readiness.application.SnapshotInput;
import com.rungs.readiness.application.response.AssessmentResponse;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** {@code POST /v1/assessments} (PRD FR-1): assesses a financial snapshot and returns the full stage ladder. */
@RestController
public class AssessmentController {

    private final AssessmentService assessmentService;

    /** Creates the controller over {@code assessmentService}. */
    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = Objects.requireNonNull(assessmentService, "assessmentService");
    }

    /** Returns the assessment of the submitted snapshot. */
    @PostMapping(
            path = "/v1/assessments",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public AssessmentResponse assess(@RequestBody SnapshotInput snapshot) {
        return AssessmentResponse.from(assessmentService.assess(snapshot));
    }

    /**
     * Interim handler until story 011 replaces it with problem details. Both kinds of unusable input become a bare 400
     * with an empty body, so no submitted value is echoed:
     *
     * <ul>
     *   <li>a body Jackson cannot bind ({@link HttpMessageNotReadableException}: malformed JSON, an unknown property,
     *       or a refused coercion). Handled here rather than by Spring's default resolver, which logs the parser
     *       message, submitted values included, at WARN (PRD NFR-2);
     *   <li>input the normalization cannot turn into a snapshot ({@link IllegalArgumentException}: unparseable money or
     *       percentage, unknown debt type, a value the domain rejects), which would otherwise be a 500.
     * </ul>
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    ResponseEntity<Void> unusableInput() {
        return ResponseEntity.badRequest().build();
    }
}
