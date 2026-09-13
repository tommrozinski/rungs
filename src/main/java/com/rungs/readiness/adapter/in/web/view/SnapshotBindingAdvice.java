package com.rungs.readiness.adapter.in.web.view;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Two code review findings, both turned into a plain 400 instead of an HTTP 500, with no figures from the
 * request and no stack trace in the body. Scoped to just {@link SnapshotFormController} and
 * {@link AssessmentPageController}, so neither handler masks a bug in another adapter.
 *
 * <ul>
 *     <li>A posted {@code debts[N]} whose index is at or beyond Spring's {@code autoGrowCollectionLimit} (256),
 *     such as {@code debts[99999].balance}, makes {@code DataBinder} throw an uncaught
 *     {@link IndexOutOfBoundsException} while constructing the {@code SnapshotInput} argument, before either
 *     controller even runs.</li>
 *     <li>{@code POST /snapshot/field} with a name that is not a real snapshot field (as the component gallery's
 *     sample fields are not) makes {@link com.rungs.readiness.adapter.in.web.view.FormFields#forBlur} throw an
 *     uncaught {@link IllegalArgumentException}.</li>
 * </ul>
 */
@ControllerAdvice(assignableTypes = {SnapshotFormController.class, AssessmentPageController.class})
class SnapshotBindingAdvice {

    private static final String MESSAGE = "The request could not be processed.";

    @ExceptionHandler({IndexOutOfBoundsException.class, IllegalArgumentException.class})
    ResponseEntity<String> invalidRequest(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_PLAIN).body(MESSAGE);
    }
}
