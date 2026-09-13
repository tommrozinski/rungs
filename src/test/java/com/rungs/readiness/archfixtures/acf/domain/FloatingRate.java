package com.rungs.readiness.archfixtures.acf.domain;

import java.math.BigDecimal;
import java.util.List;

/** AC-F violation fixture: floating point in each banned position. */
public final class FloatingRate {

    private final double rate;

    private final List<Float> history;

    public FloatingRate(double rate, List<Float> history) {
        this.rate = rate;
        this.history = List.copyOf(history);
    }

    public static long scaled(float factor) {
        return (long) factor;
    }

    public Double boxed() {
        return rate;
    }

    public long fromDecimal(BigDecimal amount) {
        return (long) amount.doubleValue();
    }

    public int historySize() {
        return history.size();
    }
}
