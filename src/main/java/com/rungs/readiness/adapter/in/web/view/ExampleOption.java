package com.rungs.readiness.adapter.in.web.view;

/**
 * One "Load an example" choice (027 REQ-008, story 3): the {@link com.rungs.readiness.application.ExampleHouseholds}
 * id to post to, and the label shown on its button.
 */
public record ExampleOption(String id, String label) {
}
