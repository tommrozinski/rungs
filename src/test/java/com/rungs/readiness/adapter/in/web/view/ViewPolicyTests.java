package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * 034 REQ-003, REQ-005: every rendered page carries the CSP-safe htmx-config meta tag and the noscript notice.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ViewPolicyTests {

    private static final String EXPECTED_HTMX_CONFIG = "{\"allowEval\":false,\"includeIndicatorStyles\":false,"
            + "\"responseHandling\":[{\"code\":\"204\",\"swap\":false},{\"code\":\"[23]..\",\"swap\":true},"
            + "{\"code\":\"400\",\"swap\":true},{\"code\":\"422\",\"swap\":true},"
            + "{\"code\":\"[45]..\",\"swap\":false,\"error\":true}]}";

    private static final String EXPECTED_NOSCRIPT = "Rungs needs JavaScript turned on.";

    private final MockMvcTester mvc;

    @Autowired
    ViewPolicyTests(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    void theSnapshotFormCarriesTheHtmxConfigMetaAndNoscriptNotice() {
        assertHtmxConfigAndNoscript(mvc.get().uri("/").exchange());
    }

    @Test
    void theAssessmentPageCarriesTheHtmxConfigMetaAndNoscriptNotice() {
        assertHtmxConfigAndNoscript(mvc.post().uri("/assessment").param("monthlyTakeHomeIncome", "5000").exchange());
    }

    @Test
    void everyHandWrittenScriptIsDeferred() {
        Document page = document(mvc.get().uri("/").exchange());

        for (Element script : page.select("script[src]")) {
            assertThat(script.hasAttr("defer")).as("script %s is deferred", script.attr("src")).isTrue();
        }
    }

    private static void assertHtmxConfigAndNoscript(MvcTestResult result) {
        Document page = document(result);

        Element meta = page.selectFirst("meta[name=htmx-config]");
        assertThat(meta).as("htmx-config meta tag").isNotNull();
        assertThat(meta.attr("content")).isEqualTo(EXPECTED_HTMX_CONFIG);

        Element noscript = page.selectFirst("noscript");
        assertThat(noscript).as("noscript notice").isNotNull();
        assertThat(noscript.text()).isEqualTo(EXPECTED_NOSCRIPT);
    }

    private static Document document(MvcTestResult result) {
        try {
            return Jsoup.parse(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        } catch (java.io.UnsupportedEncodingException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
