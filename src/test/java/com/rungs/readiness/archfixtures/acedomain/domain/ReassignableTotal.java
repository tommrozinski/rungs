package com.rungs.readiness.archfixtures.acedomain.domain;

/** AC-E violation fixture: a final domain class whose field can be reassigned, and an anonymous class with one. */
public final class ReassignableTotal {

    private long cents;

    public void add(long more) {
        cents += more;
    }

    public Runnable counter() {
        return new Runnable() {
            private int runs;

            @Override
            public void run() {
                runs++;
            }
        };
    }
}
