package com.rungs.readiness.adapter.in.web.view;

import com.rungs.readiness.application.ExampleHouseholds;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The button label shown for each {@link ExampleHouseholds} id (027 REQ-008, story 3). */
final class ExampleOptions {

    private static final Map<String, String> LABELS = labels();

    private ExampleOptions() {
    }

    /** Returns every example option, in {@link ExampleHouseholds#ids()} order. */
    static List<ExampleOption> all() {
        return ExampleHouseholds.ids().stream().map(id -> new ExampleOption(id, label(id))).toList();
    }

    private static String label(String id) {
        String label = LABELS.get(id);
        return label == null ? id : label;
    }

    private static Map<String, String> labels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("deep-in-debt", "Deep in debt");
        labels.put("mid-journey", "Mid-journey");
        labels.put("nearly-done", "Nearly done");
        return labels;
    }
}
