package com.rungs.readiness.config;

import com.rungs.readiness.application.AssessmentService;
import com.rungs.readiness.application.ProjectionService;
import com.rungs.readiness.application.validation.SnapshotValidator;
import com.rungs.readiness.domain.stage.CollegeEvaluator;
import com.rungs.readiness.domain.stage.DebtFreeEvaluator;
import com.rungs.readiness.domain.stage.FullEmergencyFundEvaluator;
import com.rungs.readiness.domain.stage.MortgageEvaluator;
import com.rungs.readiness.domain.stage.RetirementEvaluator;
import com.rungs.readiness.domain.stage.StageLadder;
import com.rungs.readiness.domain.stage.StarterFundEvaluator;
import com.rungs.readiness.domain.stage.WealthBuildingEvaluator;
import jakarta.validation.Validator;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the framework-free application services; {@code application} and {@code domain} carry no Spring stereotypes. */
@Configuration(proxyBeanMethods = false)
public class ApplicationConfiguration {

    /** The ladder of all seven stage evaluators, built once and shared by every service that needs the current stage. */
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

    /** The assessment service over the shared {@link StageLadder}. */
    @Bean
    AssessmentService assessmentService(StageLadder stageLadder) {
        return new AssessmentService(stageLadder);
    }

    /** The projection service (story 010) over the shared {@link StageLadder}. */
    @Bean
    ProjectionService projectionService(StageLadder stageLadder) {
        return new ProjectionService(stageLadder);
    }

    /** The single validation entry point for a submitted snapshot (story 011 review finding), over Spring's Validator. */
    @Bean
    SnapshotValidator snapshotValidator(Validator validator) {
        return new SnapshotValidator(validator);
    }
}
