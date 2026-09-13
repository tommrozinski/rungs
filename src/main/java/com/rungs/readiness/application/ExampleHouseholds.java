package com.rungs.readiness.application;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The fixed example household presets (design §3, "Load an example"), keyed by id. A deviation from 027 REQ-008:
 * with story 021 (CSV import) deferred, presets live here instead of {@code src/main/resources/examples/*.csv}.
 */
public final class ExampleHouseholds {

    private static final Map<String, SnapshotInput> PRESETS = presets();

    private ExampleHouseholds() {
    }

    /** Returns the ids of the three presets, in a fixed order. */
    public static List<String> ids() {
        return List.copyOf(PRESETS.keySet());
    }

    /** Returns the preset for {@code id}, or empty for a null or unknown id. */
    public static Optional<SnapshotInput> byId(String id) {
        return Optional.ofNullable(id).map(PRESETS::get);
    }

    private static Map<String, SnapshotInput> presets() {
        Map<String, SnapshotInput> presets = new LinkedHashMap<>();
        presets.put("deep-in-debt", deepInDebt());
        presets.put("mid-journey", midJourney());
        presets.put("nearly-done", nearlyDone());
        return Collections.unmodifiableMap(presets);
    }

    private static SnapshotInput deepInDebt() {
        List<DebtInput> debts = List.of(
                new DebtInput("Store card", "CREDIT_CARD", "1850", "24.99", "55"),
                new DebtInput("Car loan", "AUTO", "9400", "6.9", "285"),
                new DebtInput("Student loan", "STUDENT_LOAN", "21300", "5.5", "240"));
        return new SnapshotInput("4200", "3100", "400", "400", "0", "0", 3, debts, null,
                new HouseholdFlagsInput(true, false, false));
    }

    private static SnapshotInput midJourney() {
        MortgageInput mortgage = new MortgageInput("186000", "6.25", "1450", "310000");
        return new SnapshotInput("6800", "4200", "1000", "12600", "0", "8.5", 3, List.of(), mortgage,
                new HouseholdFlagsInput(false, false, true));
    }

    private static SnapshotInput nearlyDone() {
        MortgageInput mortgage = new MortgageInput("42000", "3.75", "900", "420000");
        return new SnapshotInput("9500", "5000", "1000", "30000", "250", "15", 6, List.of(), mortgage,
                new HouseholdFlagsInput(false, false, true));
    }
}
