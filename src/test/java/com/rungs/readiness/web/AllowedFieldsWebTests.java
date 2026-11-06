package com.rungs.readiness.web;

import static com.rungs.readiness.web.FormParams.form;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;

/**
 * The controller's {@code @InitBinder} allow-list: every real {@link SnapshotForm} field still binds normally, and
 * a field outside the list — whether a class-pollution attempt like {@code class.module} or simply a field the form
 * has no property for — is silently ignored rather than bound or rejected with an error.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockTestConfig.class)
class AllowedFieldsWebTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void everyRealFormFieldStillBindsThroughTheAllowList() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .params(form()
                                .monthlyTakeHomeIncome("6200")
                                .monthlyEssentialExpenses("2100")
                                .starterEmergencyFund("1000")
                                .fullEmergencyFund("0")
                                .retirementContributionPercent("6")
                                .rent("1400")
                                .intendsToFundCollege()
                                .retired()
                                .debt(0, "Store card", "CREDIT_CARD", "840", "26.99", "35")
                                .values()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("value=\"6200\"");
        assertThat(body).contains("value=\"Store card\"");
        assertThat(body).contains("value=\"840\"");
    }

    @Test
    void aClassPollutionAttemptFieldIsIgnoredRatherThanBoundOrRejected() throws Exception {
        mockMvc.perform(post("/assess")
                        .params(form().monthlyTakeHomeIncome("6200").values())
                        .param("class.module.classLoader.resources.context.parent.pipeline.first.pattern", "x"))
                .andExpect(status().isOk());
    }

    @Test
    void theBinderActuallySuppressesAClassPollutionPathRatherThanJustHappeningToReturnTwoHundred() {
        String pollutionPath = "class.module.classLoader.resources.context.parent.pipeline.first.pattern";
        SnapshotController controller = new SnapshotController(null, Clock.systemUTC(), null);
        WebDataBinder binder = new WebDataBinder(new SnapshotForm(), "form");
        controller.initBinder(binder);
        MutablePropertyValues propertyValues = new MutablePropertyValues();
        propertyValues.add(pollutionPath, "x");
        propertyValues.add("monthlyTakeHomeIncome", "6200");

        binder.bind(propertyValues);

        BindingResult result = binder.getBindingResult();
        assertThat(result.getSuppressedFields()).contains(pollutionPath);
        assertThat(((SnapshotForm) binder.getTarget()).getMonthlyTakeHomeIncome()).isEqualTo("6200");
    }

    @Test
    void anUnknownFieldIsIgnoredRatherThanCausingAnError() throws Exception {
        mockMvc.perform(post("/assess")
                        .params(form().monthlyTakeHomeIncome("6200").values())
                        .param("foo", "bar"))
                .andExpect(status().isOk());
    }
}
