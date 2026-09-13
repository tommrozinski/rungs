package com.rungs.readiness.archfixtures.acefields.application;

/** AC-E violation fixture: public mutable state outside the domain. */
public final class PublicCounter {

    /** A public constant, which is allowed. */
    public static final int LIMIT = 10;

    public int count;
}
