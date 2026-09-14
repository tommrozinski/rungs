package com.rungs.readiness.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rungs.readiness.application.response.MoneyResponse;
import com.rungs.readiness.application.response.MonthlyPeriodResponse;
import com.rungs.readiness.application.response.ProjectionResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 030: the figure the projection page needs beyond {@link ProjectionResponse} (AC-G). */
class ProjectionPageTests {

    private static ProjectionResponse response(List<MonthlyPeriodResponse> schedule) {
        return new ProjectionResponse(null, 0, MoneyResponse.of(com.rungs.readiness.domain.model.Money.ZERO), false,
                List.of(), List.of(), schedule, List.of());
    }

    @Test
    void firstRetirementMonthIsTheFirstMonthAnyDebtRetires() {
        ProjectionResponse response = response(List.of(
                new MonthlyPeriodResponse(1, List.of(), List.of()),
                new MonthlyPeriodResponse(2, List.of(), List.of("debt-2")),
                new MonthlyPeriodResponse(3, List.of(), List.of("debt-1"))));

        ProjectionPage page = ProjectionPage.from(response);

        assertThat(page.firstRetirementMonth()).isEqualTo(2);
    }

    @Test
    void firstRetirementMonthIsNullWhenNoDebtEverRetires() {
        ProjectionResponse response = response(List.of(new MonthlyPeriodResponse(1, List.of(), List.of())));

        assertThat(ProjectionPage.from(response).firstRetirementMonth()).isNull();
    }

    @Test
    void firstRetirementMonthIsNullWithAnEmptySchedule() {
        assertThat(ProjectionPage.from(response(List.of())).firstRetirementMonth()).isNull();
    }

    @Test
    void carriesTheSameResponseTheApiSerializes() {
        ProjectionResponse response = response(List.of());

        assertThat(ProjectionPage.from(response).projection()).isEqualTo(response);
    }

    @Test
    void rejectsNullArguments() {
        assertThatThrownBy(() -> ProjectionPage.from(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProjectionPage(null, null)).isInstanceOf(NullPointerException.class);
    }
}
