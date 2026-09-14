package com.rungs.readiness.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SnapshotControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getRootRendersEmptyFormWithOneDebtRow() throws Exception {
        String body = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("<form");
        assertThat(body).contains("action=\"/assess\"");
        assertThat(body).contains("name=\"monthlyTakeHomeIncome\"");
        assertThat(body).contains("name=\"monthlyEssentialExpenses\"");
        assertThat(body).contains("name=\"starterEmergencyFund\"");
        assertThat(body).contains("name=\"fullEmergencyFund\"");
        assertThat(body).contains("name=\"retirementContributionPercent\"");
        assertThat(body).contains("name=\"rents\"");
        assertThat(body).contains("name=\"intendsToFundCollege\"");
        assertThat(body).contains("name=\"retired\"");
        assertThat(body).contains("debts[0].label");
        assertThat(body).doesNotContain("debts[1].label");
        assertThat(body).doesNotContain("<script");
    }

    @Test
    void addDebtKeepsTypedValuesAndAddsARow() throws Exception {
        String body = mockMvc.perform(post("/debts/add")
                        .param("monthlyTakeHomeIncome", "6200")
                        .param("debts[0].label", "Store card")
                        .param("debts[0].type", "CREDIT_CARD")
                        .param("debts[0].balance", "840")
                        .param("debts[0].annualInterestRate", "26.99")
                        .param("debts[0].minimumPayment", "35"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("value=\"6200\"");
        assertThat(body).contains("value=\"Store card\"");
        assertThat(body).contains("value=\"840\"");
        assertThat(body).contains("value=\"26.99\"");
        assertThat(body).contains("value=\"35\"");
        assertThat(body).contains("debts[1].label");
    }

    @Test
    void removeDropsTheRightRowAndKeepsOthers() throws Exception {
        String body = mockMvc.perform(post("/debts/remove/0")
                        .param("debts[0].label", "Store card")
                        .param("debts[1].label", "Car loan"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("value=\"Car loan\"");
        assertThat(body).doesNotContain("value=\"Store card\"");
        assertThat(body).contains("debts[0].label");
        assertThat(body).doesNotContain("debts[1].label");
    }

    @Test
    void removeWithOutOfRangeIndexDoesNotFail() throws Exception {
        mockMvc.perform(post("/debts/remove/99")
                        .param("debts[0].label", "Store card"))
                .andExpect(status().isOk());
    }

    @Test
    void largeDebtIndexDoesNotFail() throws Exception {
        mockMvc.perform(post("/assess")
                        .param("debts[99999].label", "Anything"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void assessEchoesSubmittedValuesAndCheckboxStates() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .param("monthlyTakeHomeIncome", "6200")
                        .param("monthlyEssentialExpenses", "3500")
                        .param("starterEmergencyFund", "1000")
                        .param("fullEmergencyFund", "0")
                        .param("retirementContributionPercent", "6")
                        .param("rents", "true")
                        .param("debts[0].label", "Store card")
                        .param("debts[0].type", "CREDIT_CARD")
                        .param("debts[0].balance", "840")
                        .param("debts[0].annualInterestRate", "26.99")
                        .param("debts[0].minimumPayment", "35"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("monthlyTakeHomeIncome=6200");
        assertThat(body).contains("monthlyEssentialExpenses=3500");
        assertThat(body).contains("starterEmergencyFund=1000");
        assertThat(body).contains("fullEmergencyFund=0");
        assertThat(body).contains("retirementContributionPercent=6");
        assertThat(body).contains("rents=true");
        assertThat(body).contains("intendsToFundCollege=false");
        assertThat(body).contains("retired=false");
        assertThat(body).contains("debts[0].label=Store card");
        assertThat(body).contains("debts[0].type=CREDIT_CARD");
        assertThat(body).contains("debts[0].balance=840");
        assertThat(body).contains("debts[0].annualInterestRate=26.99");
        assertThat(body).contains("debts[0].minimumPayment=35");
    }

    @Test
    void cssIsServedAtItsPath() throws Exception {
        mockMvc.perform(get("/css/rungs.css"))
                .andExpect(status().isOk());
    }
}
