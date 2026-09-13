package com.rungs.readiness.adapter.in.web.view;

import com.rungs.readiness.application.AssessmentPage;
import com.rungs.readiness.application.AssessmentService;
import com.rungs.readiness.application.SnapshotInput;
import com.rungs.readiness.application.validation.FieldRejection;
import com.rungs.readiness.application.validation.SnapshotValidator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * S-02, the assessment screen (story 029): {@code POST /assessment} validates the submitted snapshot through
 * {@link SnapshotValidator}, the one entry point; a failure re-renders {@code snapshot/form.html} the same way a
 * failed {@code POST /} does (REQ-005), and success renders {@code assessment/page.html} from the same
 * {@link AssessmentPage} the assessment application service builds in-process (REQ-001).
 */
@Controller
class AssessmentPageController {

    private static final String PAGE_VIEW = "assessment/page";

    private final AssessmentService assessmentService;

    private final SnapshotValidator snapshotValidator;

    AssessmentPageController(AssessmentService assessmentService, SnapshotValidator snapshotValidator) {
        this.assessmentService = Objects.requireNonNull(assessmentService, "assessmentService");
        this.snapshotValidator = Objects.requireNonNull(snapshotValidator, "snapshotValidator");
    }

    @PostMapping("/assessment")
    String assess(@ModelAttribute SnapshotInput submitted, Model model) {
        List<FieldRejection> violations = snapshotValidator.validate(submitted);
        if (!violations.isEmpty()) {
            return SnapshotFormController.render(SnapshotFormController.sanitized(submitted).forForm(), violations, model);
        }
        AssessmentPage page = assessmentService.assessPage(submitted);
        model.addAttribute("page", AssessmentPageViews.of(page, submitted.forForm()));
        return PAGE_VIEW;
    }
}
