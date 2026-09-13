package com.rungs.readiness.adapter.in.web.api;

import com.rungs.readiness.application.validation.Rejection;
import com.rungs.readiness.application.validation.ValidationCode;
import jakarta.validation.ConstraintViolation;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * The single source of API error responses (story 011 REQ-012): RFC 9457 problem details whose field errors carry a
 * JSON Pointer, a {@code ValidationCode}, and that code's message. Nothing from the request or an exception message is
 * ever written to the body or the log (REQ-010).
 */
@RestControllerAdvice
class ProblemDetailsAdvice {

    /** Pointer prefix when the snapshot is the request body itself, as on {@code /v1/assessments}. */
    static final String SNAPSHOT_AT_ROOT = "";

    private static final Logger LOG = LoggerFactory.getLogger(ProblemDetailsAdvice.class);

    private static final String VALIDATION_TYPE = "urn:rungs:problem:validation";

    private static final String VALIDATION_TITLE = "Some fields need attention.";

    private static final String MALFORMED_TYPE = "urn:rungs:problem:malformed-request";

    private static final String MALFORMED_TITLE = "The request body isn't valid JSON.";

    private static final String MALFORMED_DETAIL = "Send a JSON object that matches the request format.";

    /** RFC 9457's type for a problem with no more specific meaning than its status. */
    private static final String UNTYPED = "about:blank";

    private static final String SERVER_ERROR_DETAIL = "The request could not be completed.";

    private static final Comparator<FieldProblem> ERROR_ORDER =
            Comparator.comparing(FieldProblem::pointer, JsonPointers.ORDER).thenComparing(FieldProblem::code);

    /** A rejected body: every field-only violation, or else the whole-form violation, in pointer order. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ValidationProblem> invalidFields(MethodArgumentNotValidException exception) {
        List<FieldProblem> errors = exception.getBindingResult().getAllErrors().stream()
                .map(error -> (ConstraintViolation<?>) error.unwrap(ConstraintViolation.class))
                .map(violation -> FieldProblem.of(JsonPointers.fromViolation(SNAPSHOT_AT_ROOT,
                        violation.getPropertyPath()), Rejection.of(violation)))
                .sorted(ERROR_ORDER)
                .toList();
        return validationProblem(errors);
    }

    /**
     * A body Jackson could not bind. Handled here, before Spring's default resolver, which would log the parser message
     * and so submitted names and values at WARN. An unknown property is {@code UNKNOWN_FIELD} at its pointer; a value of
     * the wrong JSON type for a field, including a refused coercion, is {@code INVALID_TYPE} at the field's pointer;
     * anything else (a syntax error, an empty body, or a body that is not a JSON object) is a malformed request.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<?> unreadableBody(HttpMessageNotReadableException exception) {
        UnrecognizedPropertyException unknown = causeOfType(exception, UnrecognizedPropertyException.class);
        if (unknown != null) {
            return validationProblem(List.of(FieldProblem.of(
                    JsonPointers.fromJackson(SNAPSHOT_AT_ROOT, unknown.getPath()), ValidationCode.UNKNOWN_FIELD)));
        }
        MismatchedInputException mismatch = causeOfType(exception, MismatchedInputException.class);
        if (mismatch != null && !mismatch.getPath().isEmpty()) {
            return validationProblem(List.of(FieldProblem.of(
                    JsonPointers.fromJackson(SNAPSHOT_AT_ROOT, mismatch.getPath()), ValidationCode.INVALID_TYPE)));
        }
        return problem(HttpStatus.BAD_REQUEST, MALFORMED_TYPE, MALFORMED_TITLE, MALFORMED_DETAIL);
    }

    /**
     * Arithmetic that overflowed while handling a request. Validation caps every amount at $1,000,000,000.00, so an
     * assessment cannot reach this; it is a safety net that answers with a fixed 500 and logs only the exception
     * class, since an arithmetic message may describe the figures involved.
     */
    @ExceptionHandler(ArithmeticException.class)
    ResponseEntity<Problem> arithmeticFailure(ArithmeticException exception) {
        LOG.error("Request failed with {}", exception.getClass().getName());
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return problem(status, UNTYPED, status.getReasonPhrase(), SERVER_ERROR_DETAIL);
    }

    private static <T extends Throwable> T causeOfType(Throwable exception, Class<T> type) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (type.isInstance(cause)) {
                return type.cast(cause);
            }
        }
        return null;
    }

    private static ResponseEntity<ValidationProblem> validationProblem(List<FieldProblem> errors) {
        ValidationProblem problem = new ValidationProblem(VALIDATION_TYPE, VALIDATION_TITLE,
                HttpStatus.BAD_REQUEST.value(), detail(errors.size()), errors);
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }

    private static ResponseEntity<Problem> problem(HttpStatus status, String type, String title, String detail) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(new Problem(type, title, status.value(), detail));
    }

    /** "1 field needs attention." or "N fields need attention." (REQ-006's "{n} field(s)", written out in English). */
    static String detail(int fieldCount) {
        return fieldCount == 1 ? "1 field needs attention." : fieldCount + " fields need attention.";
    }

    /**
     * An RFC 9457 validation problem. A record rather than Spring's {@code ProblemDetail}, whose properties Jackson 3
     * sorts alphabetically and whose {@code instance} Spring fills in, so the members are written in this order.
     *
     * @param type always {@code urn:rungs:problem:validation}
     * @param title always "Some fields need attention."
     * @param status always 400
     * @param detail how many fields need attention
     * @param errors one entry per field, in pointer order
     */
    record ValidationProblem(String type, String title, int status, String detail, List<FieldProblem> errors) {
    }

    /**
     * An RFC 9457 problem without field errors: a malformed request (REQ-005) or a server error.
     *
     * @param type the problem type URI
     * @param title fixed copy for the type
     * @param status the HTTP status
     * @param detail fixed copy for the type
     */
    record Problem(String type, String title, int status, String detail) {
    }

    /**
     * One entry of the problem's {@code errors} array.
     *
     * @param pointer the JSON Pointer to the field
     * @param code the stable wire code
     * @param message the user-facing message for the code
     */
    record FieldProblem(String pointer, String code, String message) {

        static FieldProblem of(String pointer, Rejection rejection) {
            return new FieldProblem(pointer, rejection.code().code(), rejection.message());
        }

        static FieldProblem of(String pointer, ValidationCode code) {
            return new FieldProblem(pointer, code.code(), code.message());
        }
    }
}
