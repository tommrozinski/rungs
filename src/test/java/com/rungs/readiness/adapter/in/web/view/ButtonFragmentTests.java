package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.jsoup.nodes.Element;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** C-01 button (025 REQ-002). */
class ButtonFragmentTests {

    private static final String CALL = "<div th:replace=\"~{fragments/button :: button("
            + "label=${label}, type=${type}, variant=${variant}, state=${state}, disabled=${disabled})}\"></div>";

    @ParameterizedTest
    @CsvSource({
        "primary,   '',       btn btn-primary",
        "secondary, hover,    btn btn-secondary is-hover",
        "quiet,     focus,    btn btn-quiet is-focus",
        "primary,   disabled, btn btn-primary"
    })
    void rendersVariantAndStateClasses(String variant, String state, String classes) {
        Element button = renderButton("Check my stage", variant, state, state.equals("disabled"));

        assertThat(button.tagName()).isEqualTo("button");
        assertThat(button.className()).isEqualTo(classes);
        assertThat(button.attr("type")).isEqualTo("submit");
        assertThat(button.text()).isEqualTo("Check my stage");
        assertThat(button.hasAttr("disabled")).isEqualTo(state.equals("disabled"));
    }

    @org.junit.jupiter.api.Test
    void escapesLabelMarkup() {
        Element button = renderButton("<b>Go</b>", "primary", "", false);

        assertThat(button.select("b")).isEmpty();
        assertThat(button.text()).isEqualTo("<b>Go</b>");
    }

    private static Element renderButton(String label, String variant, String state, boolean disabled) {
        return FragmentRenderer.render(CALL, Map.of("label", label, "type", "submit", "variant", variant,
                "state", state, "disabled", disabled)).body().child(0);
    }
}
