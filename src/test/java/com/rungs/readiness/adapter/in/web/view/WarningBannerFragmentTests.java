package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** C-09 warning banner (025 REQ-002): caution for warnings, problem for validation summaries and failed imports. */
class WarningBannerFragmentTests {

    private static final String CALL = "<div th:replace=\"~{fragments/warning-banner :: warningBanner("
            + "tone=${tone}, heading=${heading}, message=${message})}\"></div>";

    @ParameterizedTest
    @CsvSource({"caution, banner-caution, banner-problem", "problem, banner-problem, banner-caution"})
    void rendersHeadingAndMessageWithToneClass(String tone, String present, String absent) {
        Element banner = render(tone, "Check this", "Your expenses are higher than your income.")
                .body().child(0);

        assertThat(banner.hasClass("banner")).isTrue();
        assertThat(banner.hasClass(present)).isTrue();
        assertThat(banner.hasClass(absent)).isFalse();
        assertThat(banner.selectFirst(".banner-heading").text()).isEqualTo("Check this");
        assertThat(banner.selectFirst(".banner-message").text()).isEqualTo("Your expenses are higher than your income.");
        assertThat(banner.selectFirst(".banner-mark").attr("aria-hidden")).isEqualTo("true");
    }

    @Test
    void escapesHeadingAndMessageMarkup() {
        Document page = render("problem", "<b>Bad</b>", "<i>file</i>");

        assertThat(page.select("b, i")).isEmpty();
        assertThat(page.selectFirst(".banner-heading").text()).isEqualTo("<b>Bad</b>");
        assertThat(page.selectFirst(".banner-message").text()).isEqualTo("<i>file</i>");
    }

    private static Document render(String tone, String heading, String message) {
        return FragmentRenderer.render(CALL, Map.of("tone", tone, "heading", heading, "message", message));
    }
}
