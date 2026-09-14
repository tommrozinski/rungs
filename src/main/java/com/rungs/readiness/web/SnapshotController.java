package com.rungs.readiness.web;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.projection.PayoffSchedule;
import com.rungs.readiness.domain.projection.PayoffScheduleCalculator;
import com.rungs.readiness.domain.stage.LadderResult;
import com.rungs.readiness.domain.stage.StageLadder;
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
class SnapshotController {

    private static final String FORM_VIEW = "form";
    private static final String RESULT_VIEW = "result";
    private static final String PAYOFF_SCHEDULE_VIEW = "payoff-schedule";

    private final StageLadder ladder;
    private final Clock clock;
    private final ITemplateEngine templateEngine;

    SnapshotController(StageLadder ladder, Clock clock, ITemplateEngine templateEngine) {
        this.ladder = ladder;
        this.clock = clock;
        this.templateEngine = templateEngine;
    }

    @InitBinder
    void initBinder(WebDataBinder binder) {
        // Keeps a stray debts[99999] (or one row past the cap) from asking the binder to grow the list that far;
        // an index past the limit trips this and reaches handleUnreachableDebtIndex() as a 400.
        binder.setAutoGrowCollectionLimit(SnapshotFormMapper.MAX_DEBT_ROWS);
    }

    /**
     * Adds every {@link DebtTypeOption} and {@link SampleHousehold} to the model of every request this controller
     * handles, so the form template's type and sample selects always have the full, current list to render — even
     * on the handlers (e.g. {@code /debts/add}) that never touch either directly.
     */
    @ModelAttribute("debtTypeOptions")
    List<DebtTypeOption> debtTypeOptions() {
        return DebtTypeOption.all();
    }

    @ModelAttribute("sampleHouseholds")
    List<SampleHousehold> sampleHouseholds() {
        return List.of(SampleHousehold.values());
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
            @RequestParam(name = "breakdown", required = false, defaultValue = "false") boolean breakdown,
            Model model) {
        FinancialSnapshot snapshot = toSnapshot(form);
        LadderResult ladderResult = ladder.assess(snapshot);
        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);
        model.addAttribute("form", form);
        model.addAttribute("result", ResultViews.build(ladderResult, snapshot, schedule));
        if (breakdown) {
            model.addAttribute("breakdownHtml", renderPayoffSchedule(schedule));
        }
        return RESULT_VIEW;
    }

    /**
     * Re-renders the snapshot form filled in with every value from the result page's hidden copy, so
     * "Change my numbers" loses nothing the household already entered.
     */
    @PostMapping("/edit")
    String edit(@ModelAttribute("form") SnapshotForm form, Model model) {
        model.addAttribute("form", form);
        return FORM_VIEW;
    }

    /**
     * Renders the standalone payoff schedule page for the posted snapshot: its own page, distinct from the result
     * page's inline breakdown iframe, reachable directly (e.g. to print or link).
     */
    @PostMapping("/breakdown")
    String breakdown(@ModelAttribute("form") SnapshotForm form, Model model) {
        FinancialSnapshot snapshot = toSnapshot(form);
        model.addAttribute("schedule", ScheduleViews.build(PayoffScheduleCalculator.schedule(snapshot)));
        return PAYOFF_SCHEDULE_VIEW;
    }

    /**
     * Renders {@code templates/payoff-schedule.html} to a standalone HTML string for the result page's iframe
     * {@code srcdoc}; Thymeleaf escapes it as an attribute value when the result template writes it out.
     */
    private String renderPayoffSchedule(PayoffSchedule schedule) {
        Context context = new Context();
        context.setVariable("schedule", ScheduleViews.build(schedule));
        return templateEngine.process(PAYOFF_SCHEDULE_VIEW, context);
    }

    /**
     * Builds the snapshot {@code form} describes as of now, from the controller's injected {@link Clock}; shared
     * by every handler that needs one, so the "as of" moment is derived exactly once per request.
     */
    private FinancialSnapshot toSnapshot(SnapshotForm form) {
        return SnapshotFormMapper.toSnapshot(form, YearMonth.now(clock));
    }

    @PostMapping("/sample")
    String loadSample(
            @ModelAttribute("form") SnapshotForm form,
            @RequestParam(name = "sample", required = false) String sampleId,
            Model model) {
        SampleHousehold.byId(sampleId).ifPresent(sample -> model.addAttribute("form", sample.form()));
        return FORM_VIEW;
    }

    /**
     * Rejects a sparse or out-of-range {@code debts[n]} index (e.g. {@code debts[99999]}, past
     * {@link SnapshotFormMapper#MAX_DEBT_ROWS} and the binder's matching {@code autoGrowCollectionLimit}) as a 400
     * instead of failing with a 500. {@link InvalidPropertyException} is Spring's exception for exactly this: a
     * bind path the target object cannot satisfy, which here is always a debt row index, never a different invalid
     * property path — the form's other fields are all plain, always-bindable strings and booleans.
     */
    @ExceptionHandler(InvalidPropertyException.class)
    ResponseEntity<String> handleUnreachableDebtIndex() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid debt index");
    }
}
