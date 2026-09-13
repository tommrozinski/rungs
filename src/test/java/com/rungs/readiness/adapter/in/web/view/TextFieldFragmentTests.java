package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** C-02 text field (025 REQ-002). */
class TextFieldFragmentTests {

    private static final String CALL = "<div th:replace=\"~{fragments/text-field :: textField(id=${id}, name=${name}, "
            + "label=${label}, value=${value}, state=${state}, errorId=${errorId}, errorMessage=${errorMessage}, "
            + "blurValidated=${blurValidated})}\">"
            + "</div>";

    @ParameterizedTest
    @ValueSource(strings = {"", "focus", "readonly"})
    void labelIsAlwaysVisibleAndTiedToTheInput(String state) {
        Document page = render(state, "", "");

        Element input = page.selectFirst("input#monthly-income");
        assertThat(page.selectFirst("label[for=monthly-income]").text()).isEqualTo("Monthly income");
        assertThat(input.attr("name")).isEqualTo("monthlyIncome");
        assertThat(input.attr("value")).isEqualTo("$4,200.00");
        assertThat(input.hasAttr("aria-invalid")).isFalse();
        assertThat(input.hasAttr("aria-describedby")).isFalse();
        assertThat(page.select(".field-error")).isEmpty();
    }

    @Test
    void focusStateAddsForcedFocusClass() {
        assertThat(render("focus", "", "").selectFirst("input").hasClass("is-focus")).isTrue();
        assertThat(render("", "", "").selectFirst("input").hasClass("is-focus")).isFalse();
    }

    @Test
    void readOnlyStateMarksTheInputReadOnly() {
        assertThat(render("readonly", "", "").selectFirst("input").hasAttr("readonly")).isTrue();
        assertThat(render("", "", "").selectFirst("input").hasAttr("readonly")).isFalse();
    }

    @Test
    void errorStateTiesTheMessageToTheInput() {
        Document page = render("error", "monthly-income-error", "Enter zero or more.");

        Element input = page.selectFirst("input");
        Element message = page.selectFirst("#monthly-income-error");
        assertThat(input.attr("aria-invalid")).isEqualTo("true");
        assertThat(input.attr("aria-describedby")).isEqualTo("monthly-income-error");
        assertThat(input.hasClass("is-error")).isTrue();
        assertThat(message.hasClass("field-error")).isTrue();
        assertThat(message.text()).isEqualTo("Enter zero or more.");
    }

    @Test
    void hasNoOnBlurWiringByDefaultAsInTheComponentGallery() {
        Element input = render("", "", "").selectFirst("input");

        assertThat(input.hasAttr("hx-post")).isFalse();
        assertThat(input.hasAttr("hx-trigger")).isFalse();
        assertThat(input.hasAttr("hx-target")).isFalse();
        assertThat(input.hasAttr("hx-swap")).isFalse();
        assertThat(input.hasAttr("hx-include")).isFalse();
        assertThat(input.hasAttr("hx-sync")).isFalse();
    }

    @Test
    void addsOnBlurWiringWhenRequestedAsInTheSnapshotForm() {
        Element input = FragmentRenderer.render(CALL, Map.of("id", "monthly-income", "name", "monthlyIncome",
                "label", "Monthly income", "value", "$4,200.00", "state", "", "errorId", "", "errorMessage", "",
                "blurValidated", true)).selectFirst("input");

        assertThat(input.attr("hx-post")).isEqualTo("/snapshot/field");
        assertThat(input.attr("hx-trigger")).isEqualTo("blur");
        assertThat(input.attr("hx-target")).isEqualTo("this");
        assertThat(input.attr("hx-swap")).isEqualTo("outerHTML");
        assertThat(input.attr("hx-include")).isEqualTo("this");
        assertThat(input.attr("hx-sync")).isEqualTo("this:replace");
    }

    @Test
    void escapesLabelAndMessageMarkup() {
        Document page = FragmentRenderer.render(CALL, Map.of("id", "x", "name", "x", "label", "<b>Income</b>",
                "value", "", "state", "error", "errorId", "x-error", "errorMessage", "<i>Bad</i>"));

        assertThat(page.select("b, i")).isEmpty();
        assertThat(page.selectFirst("label").text()).isEqualTo("<b>Income</b>");
        assertThat(page.selectFirst("#x-error").text()).isEqualTo("<i>Bad</i>");
    }

    private static Document render(String state, String errorId, String errorMessage) {
        return FragmentRenderer.render(CALL, Map.of("id", "monthly-income", "name", "monthlyIncome",
                "label", "Monthly income", "value", "$4,200.00", "state", state, "errorId", errorId,
                "errorMessage", errorMessage));
    }
}
