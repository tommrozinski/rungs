package com.rungs.readiness.web;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.stage.CollegeEvaluator;
import com.rungs.readiness.domain.stage.DebtFreeEvaluator;
import com.rungs.readiness.domain.stage.FullEmergencyFundEvaluator;
import com.rungs.readiness.domain.stage.LadderResult;
import com.rungs.readiness.domain.stage.MortgageEvaluator;
import com.rungs.readiness.domain.stage.RetirementEvaluator;
import com.rungs.readiness.domain.stage.StageLadder;
import com.rungs.readiness.domain.stage.StarterFundEvaluator;
import com.rungs.readiness.domain.stage.WealthBuildingEvaluator;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Renders the snapshot form (S-01) and, from the posted values, a real seven-stage assessment on the result page
 * (S-02).
 */
@Controller
public class SnapshotController {

    private static final String FORM_VIEW = "form";
    private static final String RESULT_VIEW = "result";

    private final StageLadder ladder;
    private final Clock clock;

    public SnapshotController(Clock clock) {
        this.clock = clock;
        this.ladder = new StageLadder(List.of(
                new StarterFundEvaluator(),
                new DebtFreeEvaluator(),
                new FullEmergencyFundEvaluator(),
                new RetirementEvaluator(),
                new CollegeEvaluator(),
                new MortgageEvaluator(),
                new WealthBuildingEvaluator()));
    }

    @InitBinder
    void initBinder(WebDataBinder binder) {
        // Keeps a stray debts[99999] from asking the binder to grow the list to that size.
        binder.setAutoGrowCollectionLimit(100);
    }

    @GetMapping("/")
    String showForm(Model model) {
        SnapshotForm form = new SnapshotForm();
        form.getDebts().add(new SnapshotForm.DebtRow());
        model.addAttribute("form", form);
        return FORM_VIEW;
    }

    @PostMapping("/debts/add")
    String addDebt(@ModelAttribute("form") SnapshotForm form) {
        form.getDebts().add(new SnapshotForm.DebtRow());
        return FORM_VIEW;
    }

    @PostMapping("/debts/remove/{index}")
    String removeDebt(@ModelAttribute("form") SnapshotForm form, @PathVariable int index) {
        if (index >= 0 && index < form.getDebts().size()) {
            form.getDebts().remove(index);
        }
        return FORM_VIEW;
    }

    @PostMapping("/assess")
    String assess(@ModelAttribute("form") SnapshotForm form, Model model) {
        YearMonth asOf = YearMonth.now(clock);
        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, asOf);
        LadderResult ladderResult = ladder.assess(snapshot);
        model.addAttribute("result", ResultViews.build(ladderResult, snapshot));
        return RESULT_VIEW;
    }

    @PostMapping("/sample")
    String loadSample(
            @ModelAttribute("form") SnapshotForm form,
            @RequestParam(name = "sample", required = false) String sampleId,
            Model model) {
        SnapshotForm sample = SampleHouseholds.byId(sampleId, YearMonth.now(clock));
        if (sample != null) {
            model.addAttribute("form", sample);
        }
        return FORM_VIEW;
    }

    @ExceptionHandler(InvalidPropertyException.class)
    ResponseEntity<String> handleUnreachableDebtIndex() {
        // A sparse or out-of-range debts[n] index (e.g. debts[99999]) hits the binder's
        // autoGrowCollectionLimit or an empty list; reject it instead of failing with a 500.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid debt index");
    }
}
