package com.rungs.readiness.adapter.in.web.view;

/**
 * One C-08 stat card: a label, the server-formatted figure, and its tone (029 REQ-002).
 *
 * @param label the stat's label
 * @param figure the formatted figure, e.g. {@code "$500.00"}
 * @param tone {@code "positive"} or {@code "neutral"}
 */
public record StatCardView(String label, String figure, String tone) {
}
