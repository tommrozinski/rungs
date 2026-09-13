package com.rungs.readiness.application.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The wording shared by the API problem body and the form's C-09 error summary (story 011 REQ-006, story 027). */
class FieldSummaryTests {

    @ParameterizedTest
    @CsvSource({
        "1, 1 field needs attention.",
        "2, 2 fields need attention.",
        "51, 51 fields need attention."
    })
    void countsFieldsInTheSingularOrPlural(int fields, String detail) {
        assertThat(FieldSummary.detail(fields)).isEqualTo(detail);
    }
}
