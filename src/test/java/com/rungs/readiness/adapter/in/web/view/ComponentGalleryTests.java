package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** 025 REQ-005: the component gallery exists only under the {@code dev} profile. */
class ComponentGalleryTests {

    private static final String GALLERY = "/dev/components";

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("dev")
    class WithDevProfile {

        private final MockMvcTester mvc;

        @Autowired
        WithDevProfile(MockMvcTester mvc) {
            this.mvc = mvc;
        }

        @Test
        void rendersEveryComponentWithTheStylesheetAndPreloadedFont() throws UnsupportedEncodingException {
            MvcTestResult result = mvc.get().uri(GALLERY).exchange();

            assertThat(result).hasStatusOk();
            Document page = Jsoup.parse(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
            assertThat(page.selectFirst("link[rel=stylesheet]").attr("href")).isEqualTo("/css/rungs.css");
            assertThat(page.selectFirst("link[rel=preload][as=font]").attr("href"))
                    .isEqualTo("/fonts/InterVariable.woff2");
            assertThat(page.select("button.btn")).hasSizeGreaterThanOrEqualTo(12);
            assertThat(page.select("button.btn.is-hover")).hasSize(3);
            assertThat(page.select("button.btn.is-focus")).hasSize(3);
            assertThat(page.select("button.btn[disabled]")).hasSize(3);
            assertThat(page.select("input.input.is-focus")).hasSize(1);
            assertThat(page.select("input.input.is-error[aria-invalid=true]")).hasSize(1);
            assertThat(page.select("input.input[readonly]")).hasSize(1);
            assertThat(page.select("fieldset.toggle-group")).hasSize(1);
            assertThat(page.select("label.check input[checked]")).hasSize(1);
            assertThat(page.select("label.check input:not([checked])")).hasSize(1);
            assertThat(page.select(".stat.is-positive")).hasSize(1);
            assertThat(page.select(".stat:not(.is-positive)")).hasSizeGreaterThanOrEqualTo(1);
            assertThat(page.select(".banner.banner-caution")).hasSize(1);
            assertThat(page.select(".banner.banner-problem")).hasSize(1);
            assertThat(page.select("table.schedule tr.is-retired")).hasSizeGreaterThanOrEqualTo(1);
            assertThat(page.select("ol.ladder > li")).hasSize(7);
            assertThat(page.select("ol.ladder > li[aria-current=step]")).hasSize(1);
            assertThat(page.select("ol.ladder .chip-satisfied, ol.ladder .chip-in-progress")).hasSize(2);
            assertThat(page.select("ol.ladder .chip-not-started")).isNotEmpty();
            assertThat(page.select("ol.ladder .chip-skipped")).isNotEmpty();
            assertThat(page.select("[role=progressbar]")).hasSize(1);
            assertThat(page.select(".verdict .verdict-line").eachText()).containsExactly(
                    "$12,480 stands between you and being debt free.",
                    "Every earlier stage is complete or doesn't apply to you.");
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    class WithoutDevProfile {

        private final MockMvcTester mvc;

        @Autowired
        WithoutDevProfile(MockMvcTester mvc) {
            this.mvc = mvc;
        }

        @Test
        void galleryDoesNotExist() {
            assertThat(mvc.get().uri(GALLERY)).hasStatus(404);
        }
    }
}
