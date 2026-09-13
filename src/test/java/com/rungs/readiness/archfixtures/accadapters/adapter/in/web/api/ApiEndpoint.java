package com.rungs.readiness.archfixtures.accadapters.adapter.in.web.api;

import com.rungs.readiness.archfixtures.accadapters.adapter.in.web.view.ViewPage;

/** AC-C violation fixture: the REST adapter depends on the view adapter. */
public record ApiEndpoint(ViewPage page) {
}
