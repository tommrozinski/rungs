package com.rungs.readiness.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.WebDataBinder;

/**
 * {@link SnapshotController#initBinder}'s field configuration directly: the allow-list every real
 * {@link SnapshotForm} bind path still needs, plus a disallow-list that blocks a class-pollution path nested
 * anywhere in the bound path — not just at the top level — since {@code debts[*].label} and its siblings are
 * plain wildcard patterns that (were there no disallow-list) would only coincidentally, not deliberately, exclude
 * such a path.
 */
class SnapshotControllerInitBinderTests {

    private static WebDataBinder bind() {
        SnapshotController controller = new SnapshotController(null, Clock.systemUTC(), null);
        WebDataBinder binder = new WebDataBinder(new SnapshotForm(), "form");
        controller.initBinder(binder);
        return binder;
    }

    @Test
    void disallowsAnyPathWithAClassSegmentAnywhereInIt() {
        WebDataBinder binder = bind();

        assertThat(binder.getDisallowedFields()).contains("*.class.*", "class.*", "*.classLoader.*");
    }

    @Test
    void stillAllowsEveryRealDebtRowField() {
        WebDataBinder binder = bind();

        assertThat(binder.getAllowedFields()).contains(
                "debts[*].label", "debts[*].type", "debts[*].balance", "debts[*].annualInterestRate",
                "debts[*].minimumPayment");
    }
}
