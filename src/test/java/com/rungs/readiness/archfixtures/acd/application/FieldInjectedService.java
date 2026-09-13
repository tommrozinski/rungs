package com.rungs.readiness.archfixtures.acd.application;

import org.springframework.beans.factory.annotation.Autowired;

/** AC-D violation fixture: a collaborator injected into a field. */
public class FieldInjectedService {

    @Autowired
    private ConstructorInjectedService collaborator;
}
