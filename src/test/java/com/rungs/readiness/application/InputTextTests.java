package com.rungs.readiness.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class InputTextTests {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t\r\n", " ", " ", " ", "  \t　 "})
    void nullEmptyAndSpaceOnlyTextIsBlank(String text) {
        assertThat(InputText.isBlank(text)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", " a ", " x ", "."})
    void textWithAnyOtherCharacterIsNotBlank(String text) {
        assertThat(InputText.isBlank(text)).isFalse();
    }

    @Test
    void stripRemovesLeadingAndTrailingSpaceCharactersOnly() {
        assertThat(InputText.strip("  7.25\t ")).isEqualTo("7.25");
        assertThat(InputText.strip("a  b")).isEqualTo("a  b");
        assertThat(InputText.strip("  ")).isEmpty();
    }
}
