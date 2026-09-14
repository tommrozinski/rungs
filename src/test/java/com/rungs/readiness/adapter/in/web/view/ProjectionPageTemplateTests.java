package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

/** 030: {@code projection/page.html} with a hand-built {@link ProjectionPageView}. */
class ProjectionPageTemplateTests {

    private static final String MARKUP = "<div th:replace=\"~{projection/page}\"></div>";

    @Test
    void h1AndTitle() {
        Document page = render(view(List.of(), scheduleView(false, false)));

        assertThat(page.selectFirst("h1").text()).isEqualTo("Your payoff plan");
        assertThat(page.selectFirst("title").text()).isEqualTo("Your payoff plan · Rungs");
    }

    @Test
    void controlFormCarriesTheStaleResponseHtmxMarkup() {
        Document page = render(view(List.of(), scheduleView(false, false)));

        Element form = page.selectFirst("#projection-controls");
        assertThat(form.attr("hx-post")).isEqualTo("/projection/results");
        assertThat(form.attr("hx-trigger")).isEqualTo("input delay:250ms");
        assertThat(form.attr("hx-target")).isEqualTo("#results");
        assertThat(form.attr("hx-sync")).isEqualTo("this:replace");
        assertThat(form.attr("action")).isEqualTo("/projection");
    }

    @Test
    void controlFormTriggerOmitsChangedSoReclickingTheSameStrategyRadioStillFiresARequest() {
        Document page = render(view(List.of(), scheduleView(false, false)));

        // htmx's "changed" modifier caches the last dispatched value per element; a radio's
        // `value` never changes, so re-selecting an already-checked strategy radio (e.g.
        // Snowball -> Avalanche -> Snowball -> Avalanche) would be silently suppressed and the
        // results would go stale. "input" alone still triggers a fresh request each click, and
        // hx-sync="this:replace" discards any now-stale in-flight requests.
        Element form = page.selectFirst("#projection-controls");
        assertThat(form.attr("hx-trigger")).doesNotContain("changed");
    }

    @Test
    void rangeInputHasTheDocumentedBoundsAndDescribesTheValueLabel() {
        Document page = render(view(List.of(), scheduleView(false, false)));

        Element range = page.selectFirst("#extraMonthlyPayment");
        assertThat(range.attr("type")).isEqualTo("range");
        assertThat(range.attr("min")).isEqualTo("0");
        assertThat(range.attr("max")).isEqualTo("2000");
        assertThat(range.attr("step")).isEqualTo("25");
        assertThat(range.attr("value")).isEqualTo("300.00");
        assertThat(range.attr("aria-describedby")).isEqualTo("extra-payment-value");
        assertThat(page.selectFirst("#extra-payment-value").text()).isEqualTo("$300 extra a month");
    }

    @Test
    void strategyToggleMarksTheSelectedOption() {
        Document page = render(view(List.of(), scheduleView(false, false)));

        assertThat(page.select("input[name=strategy][value=AVALANCHE]").attr("checked")).isEqualTo("checked");
        assertThat(page.select("input[name=strategy][value=SNOWBALL]").hasAttr("checked")).isFalse();
    }

    @Test
    void hiddenSnapshotFieldsAreInTheControlForm() {
        Document page = render(view(List.of(), scheduleView(false, false)));

        Element hidden = page.selectFirst("#projection-controls input[type=hidden][name=monthlyTakeHomeIncome]");
        assertThat(hidden.attr("value")).isEqualTo("5000");
    }

    @Test
    void resultsRegionIsLiveAndShowsStatsAndSchedule() {
        Document page = render(view(List.of(), scheduleView(false, false)));

        assertThat(page.selectFirst("#results").attr("aria-live")).isEqualTo("polite");
        assertThat(page.select(".stat-label").eachText())
                .containsExactly("Debt-free in", "Total interest", "First debt paid off");
        assertThat(page.select(".stat-figure").eachText()).containsExactly("34 months", "$1,066.76", "Month 3");
        assertThat(page.select("table.schedule")).hasSize(1);
    }

    @Test
    void warningsRenderAsCautionBanners() {
        List<WarningBannerView> warnings =
                List.of(new WarningBannerView("Check these numbers", "The minimum payment doesn't cover interest."));
        Document page = render(view(warnings, scheduleView(false, false)));

        assertThat(page.select("#warnings-heading")).hasSize(1);
        assertThat(page.selectFirst(".banner-message").text()).isEqualTo("The minimum payment doesn't cover interest.");
        assertThat(page.selectFirst(".banner").hasClass("banner-caution")).isTrue();
    }

    @Test
    void noWarningsMeansNoWarningsSection() {
        Document page = render(view(List.of(), scheduleView(false, false)));

        assertThat(page.select("#warnings-heading")).isEmpty();
    }

    @Test
    void showAllButtonAppearsOnlyWhenThereAreMoreMonths() {
        Document withButton = render(view(List.of(), scheduleView(true, false)));
        Document withoutButton = render(view(List.of(), scheduleView(false, false)));

        Element button = withButton.selectFirst("#schedule-container button");
        assertThat(button.text()).isEqualTo("Show all 36 months");
        assertThat(button.attr("hx-post")).isEqualTo("/projection/schedule");
        assertThat(button.attr("hx-include")).isEqualTo("#projection-controls");
        assertThat(button.attr("hx-target")).isEqualTo("#schedule-container");

        assertThat(withoutButton.selectFirst("#schedule-container button")).isNull();
    }

    @Test
    void truncatedFootAppearsOnlyWhenTruncated() {
        Document truncated = render(view(List.of(), scheduleView(false, true)));
        Document notTruncated = render(view(List.of(), scheduleView(false, false)));

        assertThat(truncated.select("#schedule-container").text())
                .contains("This plan doesn't finish within 360 months.");
        assertThat(notTruncated.select("#schedule-container").text())
                .doesNotContain("This plan doesn't finish within 360 months.");
    }

    private static Document render(ProjectionPageView view) {
        return FragmentRenderer.render(MARKUP, Map.of("page", view));
    }

    private static ScheduleView scheduleView(boolean showAllButton, boolean truncated) {
        ScheduleRowView row = new ScheduleRowView("Month 1", List.of("$90.00", "$3,479.97"), false);
        return new ScheduleView(List.of("Month", "Credit card payment", "Credit card balance"), List.of(row),
                showAllButton, showAllButton ? "Show all 36 months" : null, showAllButton ? 36 : 1, truncated);
    }

    private static ProjectionPageView view(List<WarningBannerView> warnings, ScheduleView schedule) {
        ProjectionResultsView results = new ProjectionResultsView("$300 extra a month", warnings,
                new StatCardView("Debt-free in", "34 months", "neutral"),
                new StatCardView("Total interest", "$1,066.76", "neutral"),
                new StatCardView("First debt paid off", "Month 3", "neutral"), schedule);
        List<HiddenField> hiddenFields = List.of(new HiddenField("monthlyTakeHomeIncome", "5000"));
        return new ProjectionPageView("Your payoff plan · Rungs", "300.00", "AVALANCHE", hiddenFields, results);
    }
}
