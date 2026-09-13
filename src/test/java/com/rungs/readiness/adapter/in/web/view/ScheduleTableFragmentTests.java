package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

/** C-10 schedule table (025 REQ-002): column and row header scopes, standard and debt-retired rows. */
class ScheduleTableFragmentTests {

    private static final String CALL = "<div th:replace=\"~{fragments/schedule-table :: scheduleTable("
            + "caption=${caption}, columns=${columns}, rows=${rows})}\"></div>";

    private static final List<String> COLUMNS = List.of("Month", "Store card", "Car loan");

    @Test
    void headersUseColumnAndRowScopes() {
        Document page = render(List.of(
                new ScheduleRowView("Month 1", List.of("$400.00", "$9,100.00"), false),
                new ScheduleRowView("Month 2", List.of("$0.00", "$8,800.00"), true)));

        assertThat(page.selectFirst("table.schedule > caption").text()).isEqualTo("Month-by-month schedule");
        assertThat(page.select("thead th[scope=col]").eachText()).containsExactly("Month", "Store card", "Car loan");
        assertThat(page.select("tbody th[scope=row]").eachText()).containsExactly("Month 1", "Month 2 debt retired");
        assertThat(page.select("tbody tr").first().select("td.num").eachText()).containsExactly("$400.00", "$9,100.00");
    }

    @Test
    void onlyDebtRetiredRowsGetTheRetiredClassAndHiddenText() {
        Elements rows = render(List.of(
                new ScheduleRowView("Month 1", List.of("$400.00", "$9,100.00"), false),
                new ScheduleRowView("Month 2", List.of("$0.00", "$8,800.00"), true))).select("tbody tr");

        assertThat(rows.get(0).hasClass("is-retired")).isFalse();
        assertThat(rows.get(0).select(".visually-hidden")).isEmpty();
        assertThat(rows.get(1).hasClass("is-retired")).isTrue();
        assertThat(rows.get(1).selectFirst("th .visually-hidden").text()).isEqualTo("debt retired");
    }

    @Test
    void escapesCellMarkup() {
        Element row = render(List.of(new ScheduleRowView("<b>M</b>", List.of("<i>x</i>", "y"), false)))
                .selectFirst("tbody tr");

        assertThat(row.select("b, i")).isEmpty();
        assertThat(row.selectFirst("th").text()).isEqualTo("<b>M</b>");
    }

    private static Document render(List<ScheduleRowView> rows) {
        return FragmentRenderer.render(CALL, Map.of("caption", "Month-by-month schedule", "columns", COLUMNS,
                "rows", rows));
    }
}
