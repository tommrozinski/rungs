package com.rungs.readiness.web;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.projection.PayoffSchedule;
import com.rungs.readiness.domain.projection.PayoffScheduleCalculator;
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
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Renders the snapshot form and, from the posted values, a real seven-stage assessment on the result page,
 * including its inline payoff schedule breakdown and the standalone {@code /breakdown} page.
 */
@Controller
public class SnapshotController {

    private static final String FORM_VIEW = "form";
    private static final String RESULT_VIEW = "result";
    private static final String PAYOFF_SCHEDULE_VIEW = "payoff-schedule";

    private final StageLadder ladder;
    private final Clock clock;
    private final ITemplateEngine templateEngine;

    public SnapshotController(Clock clock, ITemplateEngine templateEngine) {
        this.clock = clock;
        this.templateEngine = templateEngine;
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
        // Keeps a stray debts[99999] (or one row past the cap) from asking the binder to grow the list that far;
        // an index past the limit trips this and reaches handleUnreachableDebtIndex() as a 400.
        binder.setAutoGrowCollectionLimit(SnapshotFormMapper.MAX_DEBT_ROWS);
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
        if (form.getDebts().size() < SnapshotFormMapper.MAX_DEBT_ROWS) {
            form.getDebts().add(new SnapshotForm.DebtRow());
        }
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
    String assess(
            @ModelAttribute("form") SnapshotForm form,
            @RequestParam(name = "breakdown", required = false) String breakdown,
            Model model) {
        YearMonth asOf = YearMonth.now(clock);
        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, asOf);
        LadderResult ladderResult = ladder.assess(snapshot);
        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);
        model.addAttribute("form", form);
        model.addAttribute("result", ResultViews.build(ladderResult, snapshot, schedule));
        if ("true".equals(breakdown)) {
            model.addAttribute("breakdownHtml", renderPayoffSchedule(schedule));
        }
        return RESULT_VIEW;
    }

    /** Re-renders the snapshot form filled in with every value from the result page's hidden copy, so
     * "Change my numbers" loses nothing the household already entered. */
    @PostMapping("/edit")
    String edit(@ModelAttribute("form") SnapshotForm form, Model model) {
        model.addAttribute("form", form);
        return FORM_VIEW;
    }

    /** Renders the standalone payoff schedule page for the posted snapshot, independent of the result page's
     * inline breakdown; useful for checks and tests. */
    @PostMapping("/breakdown")
    String breakdown(@ModelAttribute("form") SnapshotForm form, Model model) {
        YearMonth asOf = YearMonth.now(clock);
        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, asOf);
        model.addAttribute("schedule", ScheduleViews.build(PayoffScheduleCalculator.schedule(snapshot)));
        return PAYOFF_SCHEDULE_VIEW;
    }

    /** Renders {@code templates/payoff-schedule.html} to a standalone HTML string for the result page's iframe
     * {@code srcdoc}; Thymeleaf escapes it as an attribute value when the result template writes it out. */
    private String renderPayoffSchedule(PayoffSchedule schedule) {
        Context context = new Context();
        context.setVariable("schedule", ScheduleViews.build(schedule));
        return templateEngine.process(PAYOFF_SCHEDULE_VIEW, context);
    }

    @PostMapping("/sample")
    String loadSample(
            @ModelAttribute("form") SnapshotForm form,
            @RequestParam(name = "sample", required = false) String sampleId,
            Model model) {
        SnapshotForm sample = SampleHouseholds.byId(sampleId);
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
