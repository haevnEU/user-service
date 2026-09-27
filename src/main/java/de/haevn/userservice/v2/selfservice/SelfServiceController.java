package de.haevn.userservice.v2.selfservice;

import de.haevn.userservice.v2.gdpr.AuditLogService;
import de.haevn.userservice.v2.model.SsoUser;
import de.haevn.userservice.v2.repository.SsoUserRepository;
import de.haevn.userservice.v2.service.MailService;
import de.haevn.userservice.v2.service.PasswordManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Log4j2
@Controller
@RequestMapping("/selfservice")
@RequiredArgsConstructor
public class SelfServiceController {

    private final SsoUserRepository userRepository;
    private final AuditLogService auditLogService;
    private final MailService mailService;
    private final PasswordManagementService passwordManagementService;

    /**
     * Zeigt das Selfservice-Dashboard an.
     */
    @GetMapping
    public String showSelfServicePage(@AuthenticationPrincipal UserDetails currentUser, Model model) {
        // Holen des echten SsoUser-Modells aus der Datenbank anhand des angemeldeten Benutzers
        SsoUser user = userRepository.findByMail(currentUser.getUsername())
            .orElseThrow(() -> new IllegalStateException("Angemeldeter Benutzer im System nicht auffindbar."));

        model.addAttribute("userFirstName", user.getFirstName());
        return "user/self-service";
    }

    /**
     * Verarbeitet die Anfrage für die DSGVO-Auskunft und triggert den E-Mail-Versand.
     */
    @PostMapping("/gdpr-request")
    public String handleGdprRequest(@AuthenticationPrincipal UserDetails currentUser, RedirectAttributes redirectAttributes) {
        try {
            SsoUser user = userRepository.findByMail(currentUser.getUsername())
                .orElseThrow(() -> new IllegalStateException("Benutzer nicht gefunden."));

            // 1. Generiere die DSGVO-Map und schreibe den Eintrag ins sso_gdpr_audit Log
            Map<String, Object> gdprReport = auditLogService.createGDPR(user, currentUser);

            // 2. Sende die Daten direkt per Mail an das Postfach des Nutzers
            mailService.sendGdprReportEmail(user.getMail(), gdprReport);

            // 3. Erfolgsmeldung für das Toast-UI mitgeben
            redirectAttributes.addFlashAttribute("successMessage",
                "Deine DSGVO-Selbstauskunft wurde erfolgreich generiert und an " + user.getMail() + " gesendet.");

        } catch (Exception e) {
            log.error("Fehler bei der Verarbeitung der DSGVO-Anfrage für den Benutzer: {}", currentUser.getUsername(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                "Deine DSGVO-Selbstauskunft konnte nicht versendet werden. Bitte versuche es später erneut.");
        }

        return "redirect:/selfservice";
    }

    /**
     * Verarbeitet das Anfordern eines neuen Passwort-Reset-Links.
     * Reagiert auf den Formular-POST der geänderten Selfservice-Kachel.
     */
    @PostMapping("/password-reset-request") // <-- NEU: Passend zum HTML-Formular
    public String handlePasswordResetRequest(@AuthenticationPrincipal UserDetails currentUser, RedirectAttributes redirectAttributes) {
        try {
            SsoUser user = userRepository.findByMail(currentUser.getUsername())
                .orElseThrow(() -> new IllegalStateException("Benutzer nicht gefunden."));

            // Generiert das Token und stößt intern den asynchronen E-Mail-Versand an
            final String token = passwordManagementService.createPasswordResetToken(user.getMail());
            mailService.sendUserPasswordReset(user, token);
            redirectAttributes.addFlashAttribute("successMessage",
                "Ein Passwort-Reset-Link wurde erfolgreich an " + user.getMail() + " versendet.");

        } catch (Exception e) {
            log.error("Fehler beim Erzeugen des Passwort-Reset-Tokens für den Benutzer: {}", currentUser.getUsername(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                "Der Passwort-Reset-Link konnte nicht generiert werden. Bitte wende dich an den Support.");
        }

        return "redirect:/selfservice";
    }
}