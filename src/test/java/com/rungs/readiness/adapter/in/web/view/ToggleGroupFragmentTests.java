package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** C-03 toggle group (025 REQ-002): two options, one selected, as a radio group. */
class ToggleGroupFragmentTests {

    private static final String CALL = "<div th:replace=\"~{fragments/toggle-group :: toggleGroup(name=${name}, "
            + "legend=${legend}, firstValue='SNOWBALL', firstLabel=${firstLabel}, secondValue='AVALANCHE', "
            + "secondLabel='Highest rate first', selected=${selected})}\"></div>";

    @ParameterizedTest
    @CsvSource({"SNOWBALL, true, false", "AVALANCHE, false, true"})
    void rendersTwoRadiosInAFieldsetWithOneSelected(String selected, boolean firstChecked, boolean secondChecked) {
        Document page = render("Smallest balance first", selected);

        Elements radios = page.select("fieldset.toggle-group input[type=radio][name=strategy]");
        assertThat(page.selectFirst("fieldset > legend.visually-hidden").text()).isEqualTo("Payoff order");
        assertThat(radios).hasSize(2);
        assertThat(radios.eachAttr("value")).containsExactly("SNOWBALL", "AVALANCHE");
        assertThat(radios.get(0).hasAttr("checked")).isEqualTo(firstChecked);
        assertThat(radios.get(1).hasAttr("checked")).isEqualTo(secondChecked);
        assertThat(page.select("label.toggle").eachText())
                .containsExactly("Smallest balance first", "Highest rate first");
    }

    @ParameterizedTest
    @CsvSource({"SNOWBALL"})
    void escapesOptionLabels(String selected) {
        Document page = render("<b>Small</b>", selected);

        assertThat(page.select("label b")).isEmpty();
        assertThat(page.select("label.toggle").first().text()).isEqualTo("<b>Small</b>");
    }

    private static Document render(String firstLabel, String selected) {
        return FragmentRenderer.render(CALL, Map.of("name", "strategy", "legend", "Payoff order",
                "firstLabel", firstLabel, "selected", selected));
    }
}
