package com.rungs.readiness.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HouseholdFlagsTests {

    @ParameterizedTest
    @CsvSource({
        "false, false",
        "true, false",
        "false, true",
        "true, true"
    })
    void exposesEachFlagAsSuppliedAndComparesByValue(boolean retired, boolean intendsToFundCollege) {
        HouseholdFlags flags = new HouseholdFlags(retired, intendsToFundCollege);

        assertThat(flags.retired()).isEqualTo(retired);
        assertThat(flags.intendsToFundCollege()).isEqualTo(intendsToFundCollege);
        assertThat(flags).isEqualTo(new HouseholdFlags(retired, intendsToFundCollege));
    }
}
