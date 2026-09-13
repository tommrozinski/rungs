package com.rungs.readiness.adapter.in.web.view;

/**
 * One C-09 caution banner above the ladder (029 REQ-003), for one {@code WarningResponse}.
 *
 * @param heading the fixed caution heading
 * @param message the warning's own user-facing sentence
 */
public record WarningBannerView(String heading, String message) {
}
