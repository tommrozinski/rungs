package com.rungs.readiness.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code schedule.css}'s rules for the payoff schedule table's frozen header row and frozen first column: a single
 * scroll container for both sticky axes, a three-way z-index stack (plain body cells, the frozen row/column, and
 * their one frozen corner cell), and an opaque background on every sticky cell so a scrolled-past row's tint never
 * shows through it; and the two iframe documents' own body rules that keep "Your loans" sized exactly and
 * "Payoff schedule" from ever showing an outer scrollbar alongside its table wrapper's own.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockTestConfig.class)
class ScheduleCssTests {

    @Autowired
    private MockMvc mockMvc;

    private String css() throws Exception {
        return mockMvc.perform(get("/css/schedule.css"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    /** Returns the declaration block (without the braces) of the first rule whose selector matches {@code
     * selectorRegex} exactly, tolerating any whitespace between tokens. Fails the assertion with a clear message
     * if no such rule exists, rather than returning an empty string a later assertion would fail on cryptically. */
    private static String ruleBlock(String css, String selectorRegex) {
        // Anchored to the start of a line: schedule.css writes one rule's selector(s) per line, immediately
        // followed by its "{", so this never matches a selector that merely appears as a substring of a longer
        // one earlier in a comma-separated selector list (e.g. "body.schedule-doc" inside
        // "html:has(body.schedule-doc), body.schedule-doc").
        Pattern pattern = Pattern.compile("(?m)^" + selectorRegex + "\\s*\\{([^}]*)}");
        Matcher matcher = pattern.matcher(css);
        assertThat(matcher.find()).describedAs("expected a rule matching /%s/ in: %s", selectorRegex, css).isTrue();
        return matcher.group(1);
    }

    @Test
    void theScheduleTableWrapperFillsItsFlexDocumentAndIsTheOneScrollContainerForBothStickyAxes() throws Exception {
        String css = css();

        String rule = ruleBlock(css, "body\\.schedule-doc \\.table-wrap");
        assertThat(rule).contains("flex: 1");
        assertThat(rule).contains("min-height: 0");
        assertThat(rule).contains("overflow: auto");
    }

    @Test
    void theScheduleDocumentIsAFullHeightFlexColumnSoTheTableWrapperIsTheOnlyScrollbar() throws Exception {
        String css = css();

        assertThat(ruleBlock(css, "html:has\\(body\\.schedule-doc\\), body\\.schedule-doc")).contains("height: 100%");
        String bodyRule = ruleBlock(css, "body\\.schedule-doc");
        assertThat(bodyRule).contains("display: flex");
        assertThat(bodyRule).contains("flex-direction: column");
    }

    @Test
    void theLoansDocumentBodyHasATightPaddingDistinctFromTheSharedBodyRule() throws Exception {
        String css = css();

        assertThat(ruleBlock(css, "body\\.loans-doc")).contains("padding: 8px 16px");
    }

    @Test
    void theHeaderRowIsStickyAndSitsAboveOrdinaryCells() throws Exception {
        String css = css();

        String rule = ruleBlock(css, "thead th");
        assertThat(rule).contains("position: sticky");
        assertThat(rule).contains("top: 0");
        assertThat(rule).contains("z-index: 2");
    }

    @Test
    void theFirstColumnIsStickyAndTheCornerCellIsAboveBothTheHeaderAndTheFirstColumn() throws Exception {
        String css = css();

        String firstColumnRule = ruleBlock(css, "table\\.schedule th:first-child, table\\.schedule tbody td:first-child");
        assertThat(firstColumnRule).contains("position: sticky");
        assertThat(firstColumnRule).contains("left: 0");
        assertThat(firstColumnRule).contains("z-index: 1");

        String cornerRule = ruleBlock(css, "table\\.schedule thead th:first-child");
        assertThat(cornerRule).contains("z-index: 3");
    }

    @Test
    void everyStickyCellHasAnOpaqueBackgroundSoRowTintsNeverShowThrough() throws Exception {
        String css = css();

        String bodyFirstColumnRule =
                ruleBlock(css, "table\\.schedule tbody td:first-child, table\\.schedule tbody th:first-child");
        assertThat(bodyFirstColumnRule).contains("background: var(--white)");

        String cornerRule = ruleBlock(css, "table\\.schedule thead th:first-child");
        assertThat(cornerRule).contains("background: #F2F2F2");
        assertThat(cornerRule).contains("z-index: 3");
    }
}
