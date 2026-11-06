package com.rungs.readiness.web;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.projection.PayoffSchedule;
import com.rungs.readiness.domain.projection.PayoffScheduleCalculator;
import com.rungs.readiness.domain.stage.LadderResult;
import com.rungs.readiness.domain.stage.StageLadder;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.http.HttpStatus;
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
 * including its inline "Your loans" and "Payoff schedule" breakdown iframes.
 */
@Controller
class SnapshotController {

    private static final String FORM_VIEW = "form";
    private static final String RESULT_VIEW = "result";
    private static final String PAYOFF_LOANS_VIEW = "payoff-loans";
    private static final String PAYOFF_SCHEDULE_VIEW = "payoff-schedule";

    /** Per-row pixel height for the result page's "Your loans" iframe: a small margin above each row's own
     *  rendered height (header, loan, and total rows all render at roughly the same height), so the computed
     *  total never clips or scrolls a household's 1 to {@link SnapshotFormMapper#MAX_DEBT_ROWS} loans, even
     *  across the small rendering differences a fallback font can introduce. */
    private static final int LOANS_IFRAME_ROW_HEIGHT_PX = 29;

    /** Extra pixel height the "Your loans" iframe's computed height allows on top of its rows: the loans
     *  document's own tight body padding, its table wrapper's border, and the iframe's own border, plus a small
     *  safety margin. */
    private static final int LOANS_IFRAME_CHROME_PX = 28;

    /** Per-row pixel height for the "Payoff schedule" iframe's fit-to-content height; its rows render at the
     *  same size as the loans table's. */
    private static final int SCHEDULE_IFRAME_ROW_HEIGHT_PX = 29;

    /** Extra pixel height the "Payoff schedule" iframe allows on top of its rows: the schedule document's body
     *  padding, its table wrapper's border, the iframe's own border, and the wrapper's horizontal scrollbar. The
     *  template caps the result at 70vh, so a long schedule scrolls inside the frame instead. */
    private static final int SCHEDULE_IFRAME_CHROME_PX = 48;

    /**
     * Every bind path {@link SnapshotForm} actually has; the {@code *} in a debt row's paths matches any row index.
     * Anything outside this list is silently dropped, not rejected. This is a wildcard allow-list, not a path-aware
     * one — {@code debts[*].label} matches any field starting with {@code debts[} and ending with {@code ].label},
     * however much (or however unrelated) sits between the two, so {@link #DISALLOWED_FIELDS} still blocks a
     * class-pollution segment (e.g. {@code class.module.classLoader...}) nested anywhere in a bound path, as a
     * second, independent layer under this allow-list rather than relying on it alone.
     */
    private static final String[] ALLOWED_FIELDS = {
        "monthlyTakeHomeIncome", "monthlyEssentialExpenses", "starterEmergencyFund", "fullEmergencyFund",
        "retirementContributionPercent", "sample", "housing", "mortgageBalance", "mortgageRate", "mortgagePayment",
        "monthlyRent", "intendsToFundCollege", "retired",
        "debts[*].label", "debts[*].type", "debts[*].balance", "debts[*].annualInterestRate",
        "debts[*].minimumPayment"
    };

    /**
     * Blocks a class-pollution bind path (e.g. Spring's well-known {@code class.module.classLoader...} and
     * {@code class.classLoader...} gadgets) no matter where in the bound path the {@code class}/{@code classLoader}
     * segment falls, as defense in depth alongside {@link #ALLOWED_FIELDS}.
     */
    private static final String[] DISALLOWED_FIELDS = {"*.class.*", "class.*", "*.classLoader.*"};

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
        binder.setAllowedFields(ALLOWED_FIELDS);
        binder.setDisallowedFields(DISALLOWED_FIELDS);
    }

    /**
     * Adds every {@link DebtTypeOption} to the model of every request this controller handles, so the form
     * template's type select always has the full, current list to render — even on the handlers (e.g.
     * {@code /debts/add}) that never touch it directly.
     */
    @ModelAttribute("debtTypeOptions")
    List<DebtTypeOption> debtTypeOptions() {
        return DebtTypeOption.all();
    }

    /**
     * Adds every {@link SampleHousehold} to the model of every request this controller handles, so the form
     * template's sample select always has the full, current list to render.
     */
    @ModelAttribute("sampleHouseholds")
    List<SampleHousehold> sampleHouseholds() {
        return List.of(SampleHousehold.values());
    }

    /**
     * Adds the debt row label's length cap to the model of every request, so the form template's label input can
     * drive its {@code maxlength} from the same constant {@link SnapshotForm.DebtRow#setLabel} caps at, rather than
     * a second, hard-coded copy of it.
     */
    @ModelAttribute("debtLabelMaxLength")
    int debtLabelMaxLength() {
        return SnapshotForm.LABEL_MAX_LENGTH;
    }

    @GetMapping("/")
    String showForm(Model model) {
        SnapshotForm form = new SnapshotForm();
        form.getDebts().add(new SnapshotForm.DebtRow());
        model.addAttribute("form", form);
        return FORM_VIEW;
    }

    @PostMapping("/sample")
    String loadSample(
            @ModelAttribute("form") SnapshotForm form,
            @RequestParam(name = "sample", required = false) String sampleId,
            Model model) {
        SampleHousehold.byId(sampleId).ifPresent(sample -> model.addAttribute("form", sample.form()));
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
        model.addAttribute("result", ResultViews.build(ladderResult, snapshot, schedule));
        if (breakdown) {
            addBreakdownAttributes(model, schedule);
        }
        return RESULT_VIEW;
    }

    /**
     * Re-renders the snapshot form filled in with every value from the result page's hidden copy, so
     * "Change my numbers" loses nothing the household already entered.
     */
    @PostMapping("/edit")
    String edit(@ModelAttribute("form") SnapshotForm form) {
        return FORM_VIEW;
    }

    /**
     * Rejects a sparse or out-of-range {@code debts[n]} index (e.g. {@code debts[99999]}, past
     * {@link SnapshotFormMapper#MAX_DEBT_ROWS} and the binder's matching {@code autoGrowCollectionLimit}) as a 400
     * rendered through the same {@code error.html} page as any other 4xx, instead of failing with a 500 or a plain
     * text body. {@link InvalidPropertyException} is Spring's exception for exactly this: a bind path the target
     * object cannot satisfy, which here is always a debt row index, never a different invalid property path — the
     * form's other fields are all plain, always-bindable strings and booleans. {@link HttpServletResponse#sendError}
     * triggers the same container error dispatch (to Boot's {@code /error}, which renders {@code error.html}) that
     * a plain 404 or 405 already goes through, rather than this handler building its own response body.
     */
    @ExceptionHandler(InvalidPropertyException.class)
    void handleUnreachableDebtIndex(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid debt index");
    }

    /**
     * Adds the result page's breakdown model attributes for {@code schedule}: whether the household has any loan
     * to show, and — only when it does — the two breakdown iframes' rendered {@code srcdoc} documents and the
     * "Your loans" iframe's computed height. A household with no debt or mortgage balance gets none of the three:
     * the result template renders its single "no debts" message instead of either iframe.
     */
    private void addBreakdownAttributes(Model model, PayoffSchedule schedule) {
        ScheduleView scheduleView = ScheduleViews.build(schedule);
        model.addAttribute("hasLoans", scheduleView.hasLoans());
        if (scheduleView.hasLoans()) {
            model.addAttribute("loansHtml", renderIframeDocument(PAYOFF_LOANS_VIEW, scheduleView));
            model.addAttribute("scheduleHtml", renderIframeDocument(PAYOFF_SCHEDULE_VIEW, scheduleView));
            model.addAttribute("loansIframeHeightPx", loansIframeHeightPx(scheduleView));
            model.addAttribute("scheduleIframeHeightPx", scheduleIframeHeightPx(scheduleView));
        }
    }

    /**
     * Renders {@code templates/<viewName>.html} to a standalone HTML string for one of the result page's two
     * breakdown iframes' {@code srcdoc}; Thymeleaf escapes it as an attribute value when the result template
     * writes it out.
     */
    private String renderIframeDocument(String viewName, ScheduleView scheduleView) {
        Context context = new Context();
        context.setVariable("schedule", scheduleView);
        return templateEngine.process(viewName, context);
    }

    /**
     * Returns the pixel height the result page's "Your loans" iframe needs to fit every row without inner
     * scrolling: a header row, one row per loan, and a total row, each at a generous fixed height, plus a fixed
     * chrome allowance for the loans document's own body padding and borders.
     */
    private static int loansIframeHeightPx(ScheduleView scheduleView) {
        int rows = scheduleView.loans().size() + 2;
        return rows * LOANS_IFRAME_ROW_HEIGHT_PX + LOANS_IFRAME_CHROME_PX;
    }

    /**
     * Returns the pixel height that fits the whole "Payoff schedule" table: a header row, one row per month, and a
     * totals row. The template takes the smaller of this and 70vh, so a short schedule leaves no empty frame below
     * its last row.
     */
    private static int scheduleIframeHeightPx(ScheduleView scheduleView) {
        int rows = scheduleView.months().size() + 2;
        return rows * SCHEDULE_IFRAME_ROW_HEIGHT_PX + SCHEDULE_IFRAME_CHROME_PX;
    }

    /**
     * Builds the snapshot {@code form} describes as of now, from the controller's injected {@link Clock}; shared
     * by every handler that needs one, so the "as of" moment is derived exactly once per request.
     */
    private FinancialSnapshot toSnapshot(SnapshotForm form) {
        return SnapshotFormMapper.toSnapshot(form, YearMonth.now(clock));
    }
}
