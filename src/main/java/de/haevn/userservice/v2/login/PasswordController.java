package de.haevn.userservice.v2.login;
import de.haevn.userservice.v2.model.SsoUser;
import de.haevn.userservice.v2.repository.SsoUserRepository;
import de.haevn.userservice.v2.service.MailService;
import de.haevn.userservice.v2.service.PasswordManagementService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class PasswordController {
    private final SsoUserRepository userRepository;
    private final PasswordManagementService passwordManagementService;
    private final MailService emailService;

    // 1. Zeigt die Maske zum Anfordern des Links an
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        return "user/forgot-password";
    }

    // 2. Verarbeitet das Absenden der E-Mail-Adresse
    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("username") String email,
        HttpServletRequest request,
        Model model) {

        userRepository.findByMail(email).ifPresent(user -> {
            String token = passwordManagementService.createPasswordResetToken(email);
            emailService.sendUserPasswordReset(user, token);
        });

        // "Defensive Notification" — Dem Angreifer nicht verraten, ob die Mail existiert
        model.addAttribute("success", "Falls die E-Mail-Adresse im System registriert ist, wurde ein Link versendet.");
        return "user/forgot-password";
    }

    // 3. Zeigt die Maske zur Eingabe des neuen Passworts an (wird aus der Mail aufgerufen)
    @GetMapping("/forgot-password/reset")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {

        boolean isValid = userRepository.findByPasswordResetToken(token)
            .map(user -> !passwordManagementService.isTokenExpired(user))
            .orElse(false);

        if (!isValid) {
            model.addAttribute("error", "Der Link ist ungültig oder bereits abgelaufen.");
            return "user/forgot-password";
        }

        model.addAttribute("token", token);
        return "user/reset-password";
    }

    // 4. Verarbeitet die Eingabe des neuen Passworts
    @PostMapping("/forgot-password/reset")
    public String handleResetPassword(@RequestParam("token") String token,
        @RequestParam("password") String password,
        Model model) {
        try {
            passwordManagementService.resetPassword(token, password);
            // Erfolgreich! Zurück zum Login
            return "redirect:/login?resetSuccess";
        } catch (Exception e) {
            model.addAttribute("error", "Das Zurücksetzen des Passworts ist fehlgeschlagen.");
            return "forgot-password";
        }
    }
}