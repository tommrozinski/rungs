package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** C-04 checkbox (025 REQ-002): on and off. */
class CheckboxFragmentTests {

    private static final String CALL = "<div th:replace=\"~{fragments/checkbox :: checkbox(id=${id}, name=${name}, "
            + "label=${label}, checked=${checked})}\"></div>";

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rendersLabelledCheckboxInEitherState(boolean checked) {
        Document page = render("You rent your home", checked);

        Element input = page.selectFirst("input[type=checkbox]#rents");
        assertThat(input.attr("name")).isEqualTo("rents");
        assertThat(input.attr("value")).isEqualTo("true");
        assertThat(input.hasAttr("checked")).isEqualTo(checked);
        assertThat(page.selectFirst("label.check[for=rents]").text()).isEqualTo("You rent your home");
    }

    @Test
    void escapesLabelMarkup() {
        Document page = render("<b>Rent</b>", false);

        assertThat(page.select("label b")).isEmpty();
        assertThat(page.selectFirst("label").text()).isEqualTo("<b>Rent</b>");
    }

    private static Document render(String label, boolean checked) {
        return FragmentRenderer.render(CALL, Map.of("id", "rents", "name", "rents", "label", label,
                "checked", checked));
    }
}
