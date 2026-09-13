package com.rungs.readiness.domain.projection;

import com.rungs.readiness.domain.model.Debt;
import java.util.List;
import java.util.Objects;

/**
 * A test-only {@link PayoffOrdering} that always returns a fixed sequence, regardless of the debts it is
 * given, so the simulator can be tested independently of story 009's strategies (008 REQ-012).
 */
final class FixedOrder implements PayoffOrdering {

    private final List<Debt> sequence;

    FixedOrder(List<Debt> sequence) {
        this.sequence = List.copyOf(Objects.requireNonNull(sequence, "sequence"));
    }

    @Override
    public List<Debt> order(List<Debt> debts) {
        return sequence;
    }
}
