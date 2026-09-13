package com.rungs.readiness.archfixtures.accadapters.adapter.in.web.view;

import com.rungs.readiness.archfixtures.accadapters.adapter.out.ai.AiClient;

/** AC-C violation fixture: the view adapter depends on the outbound AI adapter. */
public record ViewPage(AiClient client) {
}
