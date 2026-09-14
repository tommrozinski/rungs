package com.rungs.readiness.web;

import com.rungs.readiness.domain.stage.CollegeEvaluator;
import com.rungs.readiness.domain.stage.DebtFreeEvaluator;
import com.rungs.readiness.domain.stage.FullEmergencyFundEvaluator;
import com.rungs.readiness.domain.stage.MortgageEvaluator;
import com.rungs.readiness.domain.stage.RetirementEvaluator;
import com.rungs.readiness.domain.stage.StageLadder;
import com.rungs.readiness.domain.stage.StarterFundEvaluator;
import com.rungs.readiness.domain.stage.WealthBuildingEvaluator;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Supplies the {@link StageLadder} the controller assesses every snapshot against: the real seven evaluators, in
 * ladder order. A single, stateless instance serves the whole application.
 */
@Configuration
class StageLadderConfig {

    @Bean
    StageLadder stageLadder() {
        return new StageLadder(List.of(
                new StarterFundEvaluator(),
                new DebtFreeEvaluator(),
                new FullEmergencyFundEvaluator(),
                new RetirementEvaluator(),
                new CollegeEvaluator(),
                new MortgageEvaluator(),
                new WealthBuildingEvaluator()));
    }
}
