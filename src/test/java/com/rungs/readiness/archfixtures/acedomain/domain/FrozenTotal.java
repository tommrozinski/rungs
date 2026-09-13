package com.rungs.readiness.archfixtures.acedomain.domain;

import java.util.Comparator;

/** AC-E fixture: allowed shapes, a final class with only final fields, a nested enum, and an anonymous class. */
public final class FrozenTotal {

    private static final long LIMIT = 100;

    private final long cents;

    public FrozenTotal(long cents) {
        this.cents = Math.min(cents, LIMIT);
    }

    /** Switches over an enum from another class, which makes javac emit a synthetic, non-final switch-map class. */
    public static int sign(ReportedKind kind) {
        switch (kind) {
            case SAVED:
                return 1;
            default:
                return -1;
        }
    }

    /** An anonymous class, which javac never marks final, holding only final (captured) state. */
    public static Comparator<FrozenTotal> byCents() {
        return new Comparator<>() {
            @Override
            public int compare(FrozenTotal left, FrozenTotal right) {
                return Long.compare(left.cents, right.cents);
            }
        };
    }

    /** An enum, which is allowed. */
    public enum Kind { SAVED, OWED }
}
