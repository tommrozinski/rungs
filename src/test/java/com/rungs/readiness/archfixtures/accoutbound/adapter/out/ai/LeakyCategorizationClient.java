package com.rungs.readiness.archfixtures.accoutbound.adapter.out.ai;

import com.rungs.readiness.archfixtures.accoutbound.application.NarrationService;
import com.rungs.readiness.archfixtures.accoutbound.domain.port.CategorizationPort;
import com.rungs.readiness.archfixtures.accoutbound.domain.projection.PayoffSchedule;
import com.rungs.readiness.archfixtures.accoutbound.domain.stage.StageVerdict;

/**
 * AC-C violation fixture: an outbound adapter that implements its port (allowed) but also reaches the
 * application layer and domain internals.
 */
public record LeakyCategorizationClient(NarrationService service, StageVerdict verdict, PayoffSchedule schedule)
        implements CategorizationPort {
}
