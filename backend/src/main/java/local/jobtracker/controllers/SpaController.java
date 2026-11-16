package local.jobtracker.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class SpaController {
    private static final String ROLE = "/roles/{roleId:[0-9a-fA-F-]{36}}";

    @GetMapping("/")
    String root() {
        return "redirect:/today";
    }

    @GetMapping({"/today", "/roles", "/roles/new", "/settings", ROLE,
            ROLE + "/interviews/new", ROLE + "/interviews/{interviewId:[0-9a-fA-F-]{36}}",
            ROLE + "/assignments/new", ROLE + "/assignments/{assignmentId:[0-9a-fA-F-]{36}}"})
    String clientRoute() {
        return "forward:/index.html";
    }
}
