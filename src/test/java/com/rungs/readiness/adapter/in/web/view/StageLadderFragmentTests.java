package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

/** C-05 status chip, C-06 stage row, C-07 progress bar, C-11 verdict (026 REQ-009). */
class StageLadderFragmentTests {

    private static final String LADDER = "<ol th:replace=\"~{fragments/stage-ladder :: stageLadder(rows=${rows})}\"></ol>";

    private static final String LADDER_WITH_FIELDS = "<div><ol th:replace=\"~{fragments/stage-ladder :: "
            + "stageLadder(rows=${rows}, hiddenFields=~{::snapshotFields})}\"></ol>"
            + "<th:block th:remove=\"all\"><input th:fragment=\"snapshotFields\" type=\"hidden\" name=\"monthlyIncome\" "
            + "th:value=\"${income}\"></th:block></div>";

    private static final String VERDICT = "<section th:replace=\"~{fragments/verdict :: verdict(view=${verdict})}\"></section>";

    private static final String BLOCKER = "$12,480 stands between you and being debt free.";

    @Test
    void ladderIsAnOrderedListOfSevenWithExactlyOneCurrentExpandedRow() {
        Elements rows = render(ladder(2, "40.0")).select("ol.ladder > li");

        assertThat(rows).hasSize(7);
        assertThat(rows.select("[aria-current]")).hasSize(1);
        assertThat(rows.get(1).attr("aria-current")).isEqualTo("step");
        assertThat(rows.select(".is-expanded")).hasSize(1);
        assertThat(rows.get(1).hasClass("is-expanded")).isTrue();
        assertThat(rows.select(".stage-title").eachText()).containsExactly("Starter emergency fund", "Pay off debt",
                "Full emergency fund", "Save for retirement", "Save for college", "Pay off your home", "Build wealth");
    }

    @Test
    void chipsCarryTextLabelsAndStatusClassesForAllFourStatuses() {
        Elements chips = render(ladder(2, "40.0")).select("li .chip");

        assertThat(chips.eachText()).containsExactly("Done", "In progress", "Not started", "Doesn't apply",
                "Not started", "Doesn't apply", "Not started");
        assertThat(chips.get(0).hasClass("chip-satisfied")).isTrue();
        assertThat(chips.get(1).hasClass("chip-in-progress")).isTrue();
        assertThat(chips.get(2).hasClass("chip-not-started")).isTrue();
        assertThat(chips.get(3).hasClass("chip-skipped")).isTrue();
    }

    @Test
    void discsShowCheckDashOrNumberWithHiddenStageText() {
        Elements discs = render(ladder(2, "40.0")).select("li .disc");

        assertThat(discs.select("[aria-hidden=true]").eachText()).containsExactly("✓", "2", "3", "–", "5", "–", "7");
        assertThat(discs.select(".visually-hidden").eachText())
                .containsExactly("Stage 1", "Stage 2", "Stage 3", "Stage 4", "Stage 5", "Stage 6", "Stage 7");
    }

    @Test
    void everyRowShowsAReasonAndTheCurrentRowShowsTheBlocker() {
        Elements rows = render(ladder(2, "40.0")).select("ol.ladder > li");

        assertThat(rows.get(0).selectFirst(".stage-reason").text()).isEqualTo("This stage is complete.");
        assertThat(rows.get(1).select(".stage-reason")).isEmpty();
        assertThat(rows.get(1).selectFirst(".stage-open .blocker").text()).isEqualTo(BLOCKER);
        assertThat(rows.get(3).selectFirst(".stage-reason").text()).isEqualTo("You have no retirement plan to fund.");
        assertThat(rows.get(2).selectFirst(".stage-reason").text()).isEqualTo("Save three to six months of expenses.");
    }

    @Test
    void currentStageTwoPostsToThePayoffPlan() {
        Element row = render(ladder(2, "40.0")).selectFirst("li.is-expanded");

        Element form = row.selectFirst("form");
        assertThat(form.attr("method")).isEqualTo("post");
        assertThat(form.attr("action")).isEqualTo(StageRowViews.PAYOFF_PLAN_ACTION);
        assertThat(form.selectFirst("button[type=submit]").text()).isEqualTo("See the payoff plan");
        assertThat(row.parent().select("form")).hasSize(1);
    }

    @Test
    void otherCurrentStagesPostToChangeTheNumbers() {
        Element form = render(ladder(3, "0.0")).selectFirst("li.is-expanded form");

        assertThat(form.attr("action")).isEqualTo(StageRowViews.CHANGE_NUMBERS_ACTION);
        assertThat(form.selectFirst("button").text()).isEqualTo("Change my numbers");
    }

    @Test
    void callToActionCarriesHiddenFieldsWhenTheSlotIsFilled() {
        Document page = FragmentRenderer.render(LADDER_WITH_FIELDS, Map.of("rows", ladder(2, "40.0"), "income", "4200.00"));

        Elements hidden = page.select("li.is-expanded form input[type=hidden]");
        assertThat(hidden).hasSize(1);
        assertThat(hidden.first().attr("name")).isEqualTo("monthlyIncome");
        assertThat(hidden.first().attr("value")).isEqualTo("4200.00");
        assertThat(page.select("input[type=hidden]")).hasSize(1);
    }

    @Test
    void callToActionHasNoHiddenFieldsWhenTheSlotIsEmpty() {
        assertThat(render(ladder(2, "40.0")).select("input[type=hidden]")).isEmpty();
    }

    @Test
    void progressBarExposesItsValueAndSetsTheFillWidthFromTheServerString() {
        Element bar = render(ladder(2, "40.0")).selectFirst("li.is-expanded [role=progressbar]");

        assertThat(bar.attr("aria-valuemin")).isEqualTo("0");
        assertThat(bar.attr("aria-valuemax")).isEqualTo("100");
        assertThat(bar.attr("aria-valuenow")).isEqualTo("40.0");
        assertThat(bar.attr("aria-label")).isEqualTo("Pay off debt");
        assertThat(bar.hasClass("is-complete")).isFalse();
        assertThat(bar.selectFirst("rect.progress-fill").attr("width")).isEqualTo("40.0%");
        assertThat(render(ladder(2, "40.0")).select("[role=progressbar]")).hasSize(1);
    }

    @Test
    void progressBarIsCompleteAtOneHundred() {
        List<StageRowView> rows = List.of(1, 2, 3, 4, 5, 6, 7).stream()
                .map(stage -> StageRowViews.row(stage, "SATISFIED", "100.0", null, null, 7)).toList();

        Element bar = render(rows).selectFirst("[role=progressbar]");

        assertThat(bar.attr("aria-valuenow")).isEqualTo("100.0");
        assertThat(bar.hasClass("is-complete")).isTrue();
        assertThat(bar.selectFirst("rect").attr("width")).isEqualTo("100.0%");
    }

    @Test
    void verdictShowsTheCurrentBlocker() {
        Element verdict = renderVerdict(StageRowViews.verdict(2, "IN_PROGRESS", BLOCKER));

        assertThat(verdict.hasClass("verdict")).isTrue();
        assertThat(verdict.selectFirst(".verdict-line").text()).isEqualTo(BLOCKER);
    }

    @Test
    void verdictForSatisfiedStageSevenShowsFixedCopy() {
        Element verdict = renderVerdict(StageRowViews.verdict(7, "SATISFIED", null));

        assertThat(verdict.selectFirst(".verdict-line").text())
                .isEqualTo("Every earlier stage is complete or doesn't apply to you.");
    }

    @Test
    void escapesReasonAndVerdictMarkup() {
        List<StageRowView> rows = List.of(1, 2, 3, 4, 5, 6, 7).stream()
                .map(stage -> StageRowViews.row(stage, "NOT_STARTED", "0.0", "<b>Owe</b>", null, 1)).toList();

        Document ladder = render(rows);
        Element verdict = renderVerdict(new VerdictView("<i>Owe</i>"));

        assertThat(ladder.select("b")).isEmpty();
        assertThat(ladder.selectFirst(".blocker").text()).isEqualTo("<b>Owe</b>");
        assertThat(verdict.select("i")).isEmpty();
        assertThat(verdict.text()).isEqualTo("<i>Owe</i>");
    }

    /** Stage 1 done, the current stage in progress or not started, 4 and 6 skipped, the rest not started. */
    private static List<StageRowView> ladder(int current, String currentPercent) {
        return List.of(
                StageRowViews.row(1, "SATISFIED", "100.0", null, null, current),
                current == 2
                        ? StageRowViews.row(2, "IN_PROGRESS", currentPercent, BLOCKER, null, current)
                        : StageRowViews.row(2, "SATISFIED", "100.0", null, null, current),
                StageRowViews.row(3, "NOT_STARTED", "0.0",
                        current == 3 ? BLOCKER : "Save three to six months of expenses.", null, current),
                StageRowViews.row(4, "SKIPPED", "0.0", null, "You have no retirement plan to fund.", current),
                StageRowViews.row(5, "NOT_STARTED", "0.0", "Start a college fund.", null, current),
                StageRowViews.row(6, "SKIPPED", "0.0", null, "You rent, so there's no mortgage to pay down.", current),
                StageRowViews.row(7, "NOT_STARTED", "0.0", "Invest for the long term.", null, current));
    }

    private static Document render(List<StageRowView> rows) {
        return FragmentRenderer.render(LADDER, Map.of("rows", rows));
    }

    private static Element renderVerdict(VerdictView view) {
        return FragmentRenderer.render(VERDICT, Map.of("verdict", view)).selectFirst("section");
    }
}
