package com.rungs.readiness.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HouseholdFlagsTests {

    @ParameterizedTest
    @CsvSource({
        "false, false, false",
        "true, false, false",
        "false, true, false",
        "false, false, true",
        "true, true, true"
    })
    void exposesEachFlagAsSuppliedAndComparesByValue(boolean rents, boolean retired, boolean intendsToFundCollege) {
        HouseholdFlags flags = new HouseholdFlags(rents, retired, intendsToFundCollege);

        assertThat(flags.rents()).isEqualTo(rents);
        assertThat(flags.retired()).isEqualTo(retired);
        assertThat(flags.intendsToFundCollege()).isEqualTo(intendsToFundCollege);
        assertThat(flags).isEqualTo(new HouseholdFlags(rents, retired, intendsToFundCollege));
    }
}
