package com.rungs.readiness.archfixtures.accview.adapter.in.web.view;

import com.rungs.readiness.archfixtures.accview.application.AssessmentQueries;
import com.rungs.readiness.archfixtures.accview.domain.projection.MonthlyBalance;
import com.rungs.readiness.archfixtures.accview.domain.stage.LadderStep;

/**
 * AC-C violation fixture: a view controller that calls the application layer (allowed) but also reaches
 * the domain internals.
 */
public record LeakyViewController(AssessmentQueries queries, LadderStep step, MonthlyBalance balance) {
}
