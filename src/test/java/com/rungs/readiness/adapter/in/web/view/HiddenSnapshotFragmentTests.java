package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

/** 029 REQ-004: {@code fragments/hidden-snapshot} renders one hidden input per field. */
class HiddenSnapshotFragmentTests {

    private static final String MARKUP =
            "<form th:replace=\"~{fragments/hidden-snapshot :: hiddenSnapshot(fields=${fields})}\"></form>";

    @Test
    void rendersOneHiddenInputPerField() {
        List<HiddenField> fields = List.of(
                new HiddenField("monthlyTakeHomeIncome", "4200"), new HiddenField("householdFlags.rents", "true"));

        Document page = FragmentRenderer.render(MARKUP, Map.of("fields", fields));

        Elements inputs = page.select("input[type=hidden]");
        assertThat(inputs).hasSize(2);
        assertThat(inputs.get(0).attr("name")).isEqualTo("monthlyTakeHomeIncome");
        assertThat(inputs.get(0).attr("value")).isEqualTo("4200");
        assertThat(inputs.get(1).attr("name")).isEqualTo("householdFlags.rents");
        assertThat(inputs.get(1).attr("value")).isEqualTo("true");
    }

    @Test
    void rendersNothingForAnEmptyList() {
        Document page = FragmentRenderer.render(MARKUP, Map.of("fields", List.of()));

        assertThat(page.select("input[type=hidden]")).isEmpty();
    }
}
