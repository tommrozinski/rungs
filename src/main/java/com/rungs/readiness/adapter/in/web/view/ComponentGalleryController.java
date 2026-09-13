package com.rungs.readiness.adapter.in.web.view;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Renders every component in every state for visual review (025 REQ-005). The controller exists only under the
 * {@code dev} profile, so without it the route is not mapped and returns 404.
 */
@Controller
@Profile("dev")
class ComponentGalleryController {

    private static final String VIEW = "dev/components";

    @GetMapping("/dev/components")
    String gallery(Model model) {
        model.addAttribute("samples", GallerySamples.create());
        return VIEW;
    }
}
