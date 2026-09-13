package com.rungs.readiness.adapter.in.web.view;

/**
 * One hidden input feeding a call-to-action form with the snapshot (029 REQ-004): {@code fragments/hidden-snapshot}
 * renders one per entry.
 *
 * @param name the form field name ({@code SnapshotInput} property path, e.g. {@code debts[2].balance})
 * @param value the current value
 */
public record HiddenField(String name, String value) {
}
