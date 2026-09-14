package com.rungs.readiness.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * shows through it.
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

    @Test
    void theScheduleTableWrapperIsTheOneScrollContainerForBothStickyAxes() throws Exception {
        String css = css();

        assertThat(css).contains("table-wrap:has(table.schedule)");
        assertThat(css).contains("overflow: auto");
        assertThat(css).contains("max-height: 100vh");
    }

    @Test
    void theHeaderRowIsStickyAndSitsAboveOrdinaryCells() throws Exception {
        String css = css();

        assertThat(css).contains("thead th");
        assertThat(css).contains("position: sticky");
        assertThat(css).contains("top: 0");
        assertThat(css).contains("z-index: 2");
    }

    @Test
    void theFirstColumnIsStickyAndTheCornerCellIsAboveBothTheHeaderAndTheFirstColumn() throws Exception {
        String css = css();

        assertThat(css).contains("left: 0");
        assertThat(css).contains("z-index: 1");
        assertThat(css).contains("z-index: 3");
    }

    @Test
    void everyStickyCellHasAnOpaqueBackgroundSoRowTintsNeverShowThrough() throws Exception {
        String css = css();

        assertThat(css).contains("table.schedule thead th:first-child { background: #F2F2F2; z-index: 3; }");
        assertThat(css).contains(
                "table.schedule tbody td:first-child, table.schedule tbody th:first-child { background: var(--white); }");
    }
}
