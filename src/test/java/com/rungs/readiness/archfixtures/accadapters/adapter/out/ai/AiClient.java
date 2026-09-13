package com.rungs.readiness.archfixtures.accadapters.adapter.out.ai;

import com.rungs.readiness.archfixtures.accadapters.adapter.in.web.api.ApiEndpoint;

/** AC-C violation fixture: the outbound AI adapter depends on the REST adapter. */
public record AiClient(ApiEndpoint endpoint) {
}
