package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** C-08 stat card (025 REQ-002): neutral and positive, figure in tabular numerals. */
class StatCardFragmentTests {

    private static final String CALL =
            "<div th:replace=\"~{fragments/stat-card :: statCard(label=${label}, figure=${figure}, tone=${tone})}\"></div>";

    @ParameterizedTest
    @CsvSource({"neutral, false", "positive, true"})
    void rendersLabelAndTabularFigureWithToneClass(String tone, boolean positive) {
        Element card = render("Total debt", "$12,480.00", tone).body().child(0);

        assertThat(card.hasClass("stat")).isTrue();
        assertThat(card.hasClass("is-positive")).isEqualTo(positive);
        assertThat(card.selectFirst(".stat-label").text()).isEqualTo("Total debt");
        Element figure = card.selectFirst(".stat-figure");
        assertThat(figure.text()).isEqualTo("$12,480.00");
        assertThat(figure.hasClass("num")).isTrue();
    }

    @Test
    void escapesLabelMarkup() {
        Document page = render("<b>Debt</b>", "$1.00", "neutral");

        assertThat(page.select("b")).isEmpty();
        assertThat(page.selectFirst(".stat-label").text()).isEqualTo("<b>Debt</b>");
    }

    private static Document render(String label, String figure, String tone) {
        return FragmentRenderer.render(CALL, Map.of("label", label, "figure", figure, "tone", tone));
    }
}
