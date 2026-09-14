package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

/**
 * 030: the {@code /projection/results} and {@code /projection/schedule} fragment responses, including the
 * out-of-band value label and the "show all" swap.
 */
class ProjectionResultsFragmentTests {

    private static final String RESULTS_RESPONSE_MARKUP =
            "<div th:replace=\"~{fragments/projection-results :: resultsResponse(view=${view})}\"></div>";

    private static final String SCHEDULE_CONTAINER_MARKUP =
            "<div th:replace=\"~{fragments/projection-results :: scheduleContainer(schedule=${schedule})}\"></div>";

    private static final String INVALID_MARKUP = "<div th:replace=\"~{fragments/projection-results :: invalid}\"></div>";

    @Test
    void resultsResponseCarriesTheOutOfBandValueLabel() {
        ProjectionResultsView view = view();

        Document response = FragmentRenderer.render(RESULTS_RESPONSE_MARKUP, Map.of("view", view));

        Element label = response.selectFirst("#extra-payment-value");
        assertThat(label.attr("hx-swap-oob")).isEqualTo("true");
        assertThat(label.text()).isEqualTo("$300 extra a month");
    }

    @Test
    void resultsResponseAlsoCarriesTheStatsAndSchedule() {
        Document response = FragmentRenderer.render(RESULTS_RESPONSE_MARKUP, Map.of("view", view()));

        assertThat(response.select(".stat-figure").eachText())
                .containsExactly("34 months", "$1,066.76", "Month 3");
        assertThat(response.select("table.schedule")).hasSize(1);
    }

    @Test
    void scheduleContainerHasNoButtonAndEveryRow() {
        ScheduleRowView row1 = new ScheduleRowView("Month 1", List.of("$90.00", "$3,479.97"), false);
        ScheduleRowView row2 = new ScheduleRowView("Month 2", List.of("$90.00", "$3,459.94"), false);
        ScheduleView schedule =
                new ScheduleView(List.of("Month", "Credit card payment", "Credit card balance"),
                        List.of(row1, row2), false, null, 2, false);

        Document response = FragmentRenderer.render(SCHEDULE_CONTAINER_MARKUP, Map.of("schedule", schedule));

        assertThat(response.select("tbody tr")).hasSize(2);
        assertThat(response.selectFirst("button")).isNull();
        assertThat(response.selectFirst("#schedule-container")).isNotNull();
    }

    @Test
    void invalidFragmentIsACautionBanner() {
        Document response = FragmentRenderer.render(INVALID_MARKUP, Map.of());

        assertThat(response.selectFirst(".banner-message").text()).isEqualTo("Check your numbers and try again.");
    }

    private static ProjectionResultsView view() {
        ScheduleRowView row = new ScheduleRowView("Month 1", List.of("$90.00", "$3,479.97"), false);
        ScheduleView schedule =
                new ScheduleView(List.of("Month", "Credit card payment", "Credit card balance"), List.of(row),
                        false, null, 1, false);
        return new ProjectionResultsView("$300 extra a month", List.of(),
                new StatCardView("Debt-free in", "34 months", "neutral"),
                new StatCardView("Total interest", "$1,066.76", "neutral"),
                new StatCardView("First debt paid off", "Month 3", "neutral"), schedule);
    }
}
