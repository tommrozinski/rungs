package com.rungs.readiness.adapter.in.web.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Unit tests for the parts of {@link ProblemDetailsAdvice} a well-formed assessment request cannot reach. */
@ExtendWith(OutputCaptureExtension.class)
class ProblemDetailsAdviceTests {

    private final ProblemDetailsAdvice advice = new ProblemDetailsAdvice();

    @Test
    void anEscapedArithmeticFailureIsAFixedServerErrorProblem(CapturedOutput output) {
        ResponseEntity<ProblemDetailsAdvice.Problem> response =
                advice.arithmeticFailure(new ArithmeticException("overflow at 987654.32"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).isEqualTo(new ProblemDetailsAdvice.Problem("about:blank",
                "Internal Server Error", 500, "The request could not be completed."));
        assertThat(output).contains("ArithmeticException").doesNotContain("987654");
    }

    @ParameterizedTest
    @CsvSource({
        "1, 1 field needs attention.",
        "2, 2 fields need attention.",
        "51, 51 fields need attention."
    })
    void theDetailCountsFieldsInTheSingularOrPlural(int fields, String detail) {
        assertThat(ProblemDetailsAdvice.detail(fields)).isEqualTo(detail);
    }
}
