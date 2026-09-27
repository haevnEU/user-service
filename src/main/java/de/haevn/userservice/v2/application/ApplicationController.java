package de.haevn.userservice.v2.application;

import de.haevn.userservice.v2.model.SsoApplication;
import de.haevn.userservice.v2.service.ApplicationService;
import de.haevn.userservice.v2.service.UserService;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Log4j2
@Controller
@RequiredArgsConstructor
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserService userService;
    private final SecureRandom secureRandom = new SecureRandom();

    @GetMapping
    public String listApplication(Model model) {
        final List<SsoApplication> users = applicationService.findAll();
        model.addAttribute("applications", users);
        return "application/application-list";
    }

    @GetMapping("/new")
    public String showCreateForm() {
        return "application/application-create";
    }

    @PostMapping("/new")
    public String createApplication(@RequestParam String name, @RequestParam String maintainer,
        @RequestParam String redirectUrisRaw, @RequestParam(required = false) String postLogoutRedirectUrisRaw,
        @RequestParam(value = "scopes", required = false) Set<String> scopes,
        @RequestParam(value = "grantTypes", required = false) Set<String> grantTypes) {
        log.info("Verarbeite Registrierung für Anwendung: {}", name);
        userService.findByMail(maintainer).orElseThrow();
        applicationService.save(name, maintainer, redirectUrisRaw, postLogoutRedirectUrisRaw, scopes, grantTypes);
        return "redirect:/applications";
    }

    @PostMapping("/{id}/delete")
    public String deleteApplication(@PathVariable String id) {
        log.warn("Anforderung zum Löschen der Anwendung mit ID: {}", id);
        applicationService.deleteById(id);
        return "redirect:/applications";
    }
}
