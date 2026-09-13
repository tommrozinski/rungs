package com.rungs.readiness.archfixtures.acd.application;

import org.springframework.beans.factory.annotation.Autowired;

/** AC-D fixture: constructor injection, which is allowed. */
public final class ConstructorInjectedService {

    private final Object collaborator;

    @Autowired
    public ConstructorInjectedService(Object collaborator) {
        this.collaborator = collaborator;
    }
}
