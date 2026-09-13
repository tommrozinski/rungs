package com.rungs.readiness.archfixtures.acedomain.domain;

/** AC-E violation fixture: a domain class that is not final, so a subclass could add mutable state. */
public class OpenTotal {

    private final long cents;

    public OpenTotal(long cents) {
        this.cents = cents;
    }
}
