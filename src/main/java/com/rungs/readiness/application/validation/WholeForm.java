package com.rungs.readiness.application.validation;

/**
 * The validation group of whole-form rules (story 011 REQ-003, REQ-007). Its constraints are checked only after every
 * field-only rule has passed, through the input model's group sequence, so they may assume parseable fields.
 */
public interface WholeForm {
}
