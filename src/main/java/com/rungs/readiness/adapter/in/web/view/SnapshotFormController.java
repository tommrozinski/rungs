package com.rungs.readiness.adapter.in.web.view;

import com.rungs.readiness.application.DebtInput;
import com.rungs.readiness.application.ExampleHouseholds;
import com.rungs.readiness.application.SnapshotInput;
import com.rungs.readiness.application.validation.FieldRejection;
import com.rungs.readiness.application.validation.SnapshotValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * S-01, the snapshot form screen (story 027): {@code GET /} and {@code POST /} render and re-render the whole form;
 * the debts table's add and remove actions and each field's on-blur validation are separate htmx-swapped fragments;
 * "Load an example" loads one of the {@link ExampleHouseholds} presets. Every validation runs through
 * {@link SnapshotValidator}, the one entry point (story 011 review finding).
 */
@Controller
class SnapshotFormController {

    private static final String FORM_VIEW = "snapshot/form";

    private static final String DEBTS_TABLE_FRAGMENT = FORM_VIEW + " :: debtsTableContent";

    private static final String FIELD_FRAGMENT = "snapshot/field :: field(f=${field})";

    private static final String TITLE = "Check your stage · Rungs";

    private static final DebtInput BLANK_DEBT = new DebtInput(null, null, null, null, null);

    private final SnapshotValidator snapshotValidator;

    SnapshotFormController(SnapshotValidator snapshotValidator) {
        this.snapshotValidator = Objects.requireNonNull(snapshotValidator, "snapshotValidator");
    }

    /** REQ-001: an empty form. */
    @GetMapping("/")
    String blank(Model model) {
        return render(blankSnapshot().forForm(), List.of(), model);
    }

    /** REQ-001: re-renders the form from posted fields ("Change my numbers"), with every field's own errors. */
    @PostMapping("/")
    String changeNumbers(@ModelAttribute SnapshotInput submitted, Model model) {
        List<FieldRejection> violations = snapshotValidator.validate(submitted);
        return render(sanitized(submitted).forForm(), violations, model);
    }

    /**
     * REQ-004: appends a blank debt row and swaps in the debts table, unless a 51st real debt would be over the
     * limit, in which case the row is not added and the table carries the 011 {@code TOO_MANY_DEBTS} message.
     */
    @PostMapping("/snapshot/debts/add")
    String addDebt(@ModelAttribute SnapshotInput rawSubmitted, Model model) {
        SnapshotInput submitted = sanitized(rawSubmitted);
        List<DebtInput> current = debtsOf(submitted);
        List<DebtInput> tentative = new ArrayList<>(current);
        tentative.add(BLANK_DEBT);
        SnapshotInput tentativeSnapshot = withDebts(submitted, tentative);
        List<FieldRejection> violations = snapshotValidator.validateDebtCount(tentativeSnapshot);
        boolean tooMany = !violations.isEmpty();
        SnapshotInput result = tooMany ? withDebts(submitted, current) : tentativeSnapshot;
        model.addAttribute("form", SnapshotFormViews.of(result.forForm(), tooMany ? violations : List.of()));
        return DEBTS_TABLE_FRAGMENT;
    }

    /** REQ-004: removes the debt row at {@code index} and swaps in the debts table. */
    @PostMapping("/snapshot/debts/remove/{index}")
    String removeDebt(@PathVariable int index, @ModelAttribute SnapshotInput rawSubmitted, Model model) {
        SnapshotInput submitted = sanitized(rawSubmitted);
        List<DebtInput> current = new ArrayList<>(debtsOf(submitted));
        if (index >= 0 && index < current.size()) {
            current.remove(index);
        }
        model.addAttribute("form", SnapshotFormViews.of(withDebts(submitted, current).forForm(), List.of()));
        return DEBTS_TABLE_FRAGMENT;
    }

    /** REQ-005: validates the one posted field and swaps in its fragment. */
    @PostMapping("/snapshot/field")
    String field(@RequestParam Map<String, String> fields, Model model) {
        Map.Entry<String, String> posted = fields.entrySet().iterator().next();
        model.addAttribute("field", FormFields.forBlur(posted.getKey(), posted.getValue(), snapshotValidator));
        return FIELD_FRAGMENT;
    }

    /** REQ-008: loads a preset household through the same path a submitted snapshot takes; 404 for an unknown id. */
    @PostMapping("/snapshot/example/{id}")
    String example(@PathVariable String id, Model model) {
        SnapshotInput preset = ExampleHouseholds.byId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return render(preset.forForm(), List.of(), model);
    }

    /** Renders the whole form for {@code normalized} and its violations; shared with {@link AssessmentPageController} (029 REQ-005), so a failed {@code POST /assessment} re-renders the same way a failed {@code POST /} does. */
    static String render(SnapshotInput normalized, List<FieldRejection> violations, Model model) {
        model.addAttribute("title", TITLE);
        model.addAttribute("form", SnapshotFormViews.of(normalized, violations));
        model.addAttribute("examples", ExampleOptions.all());
        return FORM_VIEW;
    }

    /**
     * Code review finding: a debt list bound from a sparse post (e.g. only {@code debts[200].balance} set) has a
     * {@code null} entry at every unset index below it; {@code null} there is meaningful to {@link SnapshotValidator}
     * (it reports {@code INVALID_TYPE} at that index), but every other user of the list, including {@code forForm()},
     * assumes a real {@link DebtInput}. Returns {@code submitted} unchanged when its debts have no {@code null} entry,
     * otherwise a copy with a blank row standing in for each one, so rendering never throws a
     * {@link NullPointerException}; shared with {@link AssessmentPageController} (same review finding).
     */
    static SnapshotInput sanitized(SnapshotInput submitted) {
        List<DebtInput> debts = submitted.debts();
        if (debts == null || debts.stream().noneMatch(Objects::isNull)) {
            return submitted;
        }
        List<DebtInput> filled = new ArrayList<>(debts.size());
        for (DebtInput debt : debts) {
            filled.add(debt == null ? BLANK_DEBT : debt);
        }
        return withDebts(submitted, filled);
    }

    private static List<DebtInput> debtsOf(SnapshotInput snapshot) {
        return snapshot.debts() == null ? List.of() : snapshot.debts();
    }

    private static SnapshotInput withDebts(SnapshotInput snapshot, List<DebtInput> debts) {
        return new SnapshotInput(snapshot.monthlyTakeHomeIncome(), snapshot.monthlyEssentialExpenses(),
                snapshot.starterEmergencyFund(), snapshot.fullEmergencyFund(), snapshot.monthlyCollegeContribution(),
                snapshot.retirementContributionPercent(), snapshot.emergencyFundMonths(), List.copyOf(debts),
                snapshot.mortgage(), snapshot.householdFlags());
    }

    private static SnapshotInput blankSnapshot() {
        return new SnapshotInput(null, null, null, null, null, null, null, null, null, null);
    }
}
