package com.rungs.readiness.archfixtures.acb.domain;

import com.rungs.readiness.archfixtures.acb.adapter.in.web.api.AssessmentController;
import com.rungs.readiness.archfixtures.acb.application.AssessmentService;

/** AC-B violation fixture: a domain class that depends on the application layer and on an adapter. */
public record LayerLeakingModel(AssessmentService service, AssessmentController controller) {
}
