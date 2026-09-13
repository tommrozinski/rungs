package com.rungs.readiness.adapter.in.web.view;

import com.rungs.readiness.application.validation.Rejection;
import java.util.Objects;
import java.util.Optional;

/**
 * What one text field needs to render through {@code fragments/text-field} (C-02): its stable id (so htmx 2 restores
 * focus after an on-blur swap, story 027 REQ-005), its {@code @ModelAttribute} form field name, its label, its
 * current display value, and any {@link Rejection} tied to it by {@code aria-describedby}.
 *
 * @param id the element id, stable across renders
 * @param name the form field name ({@code SnapshotInput} property path, e.g. {@code debts[2].balance})
 * @param label the visible label
 * @param value the current display value
 * @param rejection the field's error, or empty when it has none
 */
public record FieldView(String id, String name, String label, String value, Optional<Rejection> rejection) {

    /** Creates a field view; throws NullPointerException if any component but the value is null. */
    public FieldView {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(rejection, "rejection");
    }

    /**
     * Returns {@code error} when {@link #rejection()} is present, the state {@code fragments/text-field} expects.
     * Public, like every accessor here: the template engine reaches these through reflection (Spring EL), which
     * requires public members on a type outside its own package.
     */
    public String state() {
        return rejection.isPresent() ? "error" : "";
    }

    /** Returns this field's error element id, used whether or not it currently has an error. */
    public String errorId() {
        return id + "-error";
    }

    /** Returns the error message, or blank when this field has no error. */
    public String errorMessage() {
        return rejection.map(Rejection::message).orElse("");
    }
}
