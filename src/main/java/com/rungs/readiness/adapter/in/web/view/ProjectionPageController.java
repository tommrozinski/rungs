package com.rungs.readiness.adapter.in.web.view;

import com.rungs.readiness.application.ProjectionInput;
import com.rungs.readiness.application.ProjectionPage;
import com.rungs.readiness.application.ProjectionService;
import com.rungs.readiness.application.SnapshotInput;
import com.rungs.readiness.application.validation.FieldRejection;
import com.rungs.readiness.application.validation.SnapshotValidator;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * S-03, the projection screen (story 030): {@code POST /projection} renders the whole page from an extra payment of
 * $0 and the SNOWBALL strategy (029's stage-2 call to action posts no controls); {@code POST /projection/results}
 * and {@code POST /projection/schedule} re-render the results region and the full schedule as the control card
 * changes, debounced by htmx (REQ-003). Every submission validates through {@link SnapshotValidator}, the one entry
 * point, cascaded through {@link ProjectionInput} so the extra payment and strategy are checked too.
 */
@Controller
class ProjectionPageController {

    private static final String PAGE_VIEW = "projection/page";

    private static final String RESULTS_FRAGMENT = "fragments/projection-results :: resultsResponse(view=${results})";

    private static final String SCHEDULE_FRAGMENT = "fragments/projection-results :: scheduleContainer(schedule=${schedule})";

    private static final String INVALID_FRAGMENT = "fragments/projection-results :: invalid";

    /** The prefix {@link SnapshotValidator#validate(ProjectionInput)} reports a snapshot violation's pointer at. */
    private static final String SNAPSHOT_PREFIX = "/snapshot";

    /** The horizon this screen always simulates (030 REQ-003: the assumption a story 030 screen is fixed at 360). */
    private static final int HORIZON_MONTHS = 360;

    private final ProjectionService projectionService;

    private final SnapshotValidator snapshotValidator;

    ProjectionPageController(ProjectionService projectionService, SnapshotValidator snapshotValidator) {
        this.projectionService = Objects.requireNonNull(projectionService, "projectionService");
        this.snapshotValidator = Objects.requireNonNull(snapshotValidator, "snapshotValidator");
    }

    /** REQ-001: the whole page; invalid input re-renders the snapshot form, like a failed {@code POST /assessment}. */
    @PostMapping("/projection")
    String projection(@ModelAttribute SnapshotInput submitted,
            @RequestParam(required = false) String extraMonthlyPayment,
            @RequestParam(required = false) String strategy, Model model) {
        ProjectionInput input = new ProjectionInput(submitted, extraMonthlyPayment, strategy, HORIZON_MONTHS);
        List<FieldRejection> violations = snapshotValidator.validate(input);
        if (!violations.isEmpty()) {
            return SnapshotFormController.render(
                    SnapshotFormController.sanitized(submitted).forForm(), snapshotViolations(violations), model);
        }
        ProjectionPage page = projectionService.projectPage(input);
        model.addAttribute("page", ProjectionPageViews.of(page, submitted.forForm(), extraMonthlyPayment, strategy));
        return PAGE_VIEW;
    }

    /** REQ-001, REQ-003: the results region alone; invalid input is a 400 with a small caution fragment. */
    @PostMapping("/projection/results")
    String results(@ModelAttribute SnapshotInput submitted,
            @RequestParam(required = false) String extraMonthlyPayment,
            @RequestParam(required = false) String strategy, Model model, HttpServletResponse response) {
        ProjectionInput input = new ProjectionInput(submitted, extraMonthlyPayment, strategy, HORIZON_MONTHS);
        if (!snapshotValidator.validate(input).isEmpty()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return INVALID_FRAGMENT;
        }
        ProjectionPage page = projectionService.projectPage(input);
        model.addAttribute("results", ProjectionPageViews.results(page, extraMonthlyPayment));
        return RESULTS_FRAGMENT;
    }

    /** REQ-005: every month's schedule, loaded by the "Show all" button; invalid input is a 400. */
    @PostMapping("/projection/schedule")
    String schedule(@ModelAttribute SnapshotInput submitted,
            @RequestParam(required = false) String extraMonthlyPayment,
            @RequestParam(required = false) String strategy, Model model, HttpServletResponse response) {
        ProjectionInput input = new ProjectionInput(submitted, extraMonthlyPayment, strategy, HORIZON_MONTHS);
        if (!snapshotValidator.validate(input).isEmpty()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return INVALID_FRAGMENT;
        }
        ProjectionPage page = projectionService.projectPage(input);
        model.addAttribute("schedule", ProjectionPageViews.fullSchedule(page));
        return SCHEDULE_FRAGMENT;
    }

    /**
     * {@link SnapshotValidator#validate(ProjectionInput)} reports a snapshot violation's pointer prefixed with
     * {@value #SNAPSHOT_PREFIX} (the nested component's own name); strips it so the same violations can feed
     * {@link SnapshotFormController#render}, which expects a bare snapshot's own pointers.
     */
    private static List<FieldRejection> snapshotViolations(List<FieldRejection> violations) {
        List<FieldRejection> stripped = new ArrayList<>();
        for (FieldRejection violation : violations) {
            String pointer = violation.pointer();
            String withoutPrefix = pointer.startsWith(SNAPSHOT_PREFIX) ? pointer.substring(SNAPSHOT_PREFIX.length())
                    : pointer;
            stripped.add(new FieldRejection(withoutPrefix, violation.rejection()));
        }
        return stripped;
    }
}
