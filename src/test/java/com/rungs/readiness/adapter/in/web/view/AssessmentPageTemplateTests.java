package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

/**
 * 029: {@code assessment/page.html} with a hand-built {@link AssessmentPageView}, so the warnings-present path is
 * covered even though the real assessment service reports none until story 012 (design §4).
 */
class AssessmentPageTemplateTests {

    private static final String MARKUP = "<div th:replace=\"~{assessment/page}\"></div>";

    @Test
    void sectionsAppearInVerdictStatsWarningsLadderOrder() {
        Document page = render(view(true));

        List<Element> sections = page.select("main > section");
        assertThat(sections.get(0).hasClass("verdict")).isTrue();
        assertThat(sections.get(1).attr("aria-labelledby")).isEqualTo("stats-heading");
        assertThat(sections.get(2).attr("aria-labelledby")).isEqualTo("warnings-heading");
        assertThat(sections.get(3).attr("aria-labelledby")).isEqualTo("ladder-heading");
    }

    @Test
    void h1IsYourStageAndTitleNamesTheCurrentStage() {
        Document page = render(view(false));

        assertThat(page.selectFirst("h1").text()).isEqualTo("Your stage");
        assertThat(page.selectFirst("title").text()).isEqualTo("Your stage: 2 of 7 · Rungs");
    }

    @Test
    void allThreeStatCardsRenderTheirFigures() {
        Document page = render(view(false));

        assertThat(page.select(".stat-label").eachText())
                .containsExactly("Monthly surplus", "Total debt", "Emergency fund");
        assertThat(page.select(".stat-figure").eachText()).containsExactly("$500.00", "$1,200.00", "$9,000.00");
    }

    @Test
    void warningsRenderAsCautionBanners() {
        Document page = render(view(true));

        assertThat(page.select("#warnings-heading")).hasSize(1);
        assertThat(page.select(".banner").select(".banner-message").text())
                .isEqualTo("Your expenses are higher than your income.");
        assertThat(page.selectFirst(".banner").hasClass("banner-caution")).isTrue();
    }

    @Test
    void noWarningsMeansNoWarningsSection() {
        Document page = render(view(false));

        assertThat(page.select("#warnings-heading")).isEmpty();
        assertThat(page.select(".banner")).isEmpty();
    }

    @Test
    void allSevenStagesRenderIncludingSkippedOnes() {
        Document page = render(view(false));

        assertThat(page.select("ol.ladder > li")).hasSize(7);
    }

    @Test
    void hiddenSnapshotFieldsFeedTheCurrentRowsCallToAction() {
        Document page = render(view(false));

        Element form = page.selectFirst("li.is-expanded form");
        assertThat(form.attr("action")).isEqualTo(StageRowViews.PAYOFF_PLAN_ACTION);
        assertThat(form.select("input[type=hidden][name=monthlyTakeHomeIncome]").attr("value")).isEqualTo("4200");
    }

    private static Document render(AssessmentPageView view) {
        return FragmentRenderer.render(MARKUP, Map.of("page", view));
    }

    private static AssessmentPageView view(boolean withWarning) {
        List<StageRowView> ladder = List.of(1, 2, 3, 4, 5, 6, 7).stream()
                .map(stage -> StageRowViews.row(stage, stage == 2 ? "IN_PROGRESS" : "NOT_STARTED", "0.0",
                        "Blocker " + stage, null, 2))
                .toList();
        List<WarningBannerView> warnings = withWarning
                ? List.of(new WarningBannerView("Check these numbers", "Your expenses are higher than your income."))
                : List.of();
        List<HiddenField> hiddenFields = List.of(new HiddenField("monthlyTakeHomeIncome", "4200"));
        return new AssessmentPageView("Your stage: 2 of 7 · Rungs", StageRowViews.verdict(2, "IN_PROGRESS", "Blocker 2"),
                new StatCardView("Monthly surplus", "$500.00", "positive"),
                new StatCardView("Total debt", "$1,200.00", "neutral"),
                new StatCardView("Emergency fund", "$9,000.00", "neutral"), warnings, ladder, hiddenFields);
    }
}
