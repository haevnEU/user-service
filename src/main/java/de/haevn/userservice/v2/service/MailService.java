package de.haevn.userservice.v2.service;

import de.haevn.userservice.v2.model.SsoApplication;
import de.haevn.userservice.v2.model.SsoUser;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine; // oder ITemplateEngine
import tools.jackson.databind.ObjectMapper;

@Log4j2
@RequiredArgsConstructor
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final ObjectMapper objectMapper;

    @Value("${app.branding.company-name}")
    private String companyName;

    @Value("${spring.mail.user.sender}")
    private String sender;

    public void sendApplicationRemoved(SsoApplication app) {
        Context context = new Context();
        context.setVariable("appName", app.getName());
        context.setVariable("maintainer", app.getMaintainer());
        context.setVariable("companyName", companyName);
        context.setVariable("clientId", app.getClientId());
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        context.setVariable("ssoUrl", baseUrl + "/applications");
        String htmlBody = templateEngine.process("emails/application-deleted", context);
        send(htmlBody, companyName + " - Anwendung entfernt", app.getMaintainer(), Optional.empty());
    }

    public void sendApplicationRegistered(SsoApplication app) {
        Context context = new Context();
        context.setVariable("appName", app.getName());
        context.setVariable("maintainer", app.getMaintainer());
        context.setVariable("clientId", app.getClientId());
        context.setVariable("companyName", companyName);
        context.setVariable("clientSecret", app.getClientSecret());
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        context.setVariable("ssoUrl", baseUrl + "/applications");
        String htmlBody = templateEngine.process("emails/application-created", context);
        send(htmlBody, companyName + " - Neue Anwendung registriert", app.getMaintainer(), Optional.empty());
    }

    public void sendUserWelcome(SsoUser user, String token) {
        Context context = new Context();
        context.setVariable("firstName", user.getFirstName());
        context.setVariable("lastName", user.getLastName());
        context.setVariable("companyName", companyName);

        // Generiert z.B. "http://localhost:8080" oder die echte Produktions-Domain
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        context.setVariable("ssoUrl", baseUrl);

        // Reicht das Token an das Thymeleaf-Template weiter
        context.setVariable("token", token);

        // Verweist auf dein hochgeladenes Template (Pfade anpassen, falls es unter 'emails/' liegt)
        String htmlBody = templateEngine.process("emails/user-welcome", context);

        send(htmlBody, "Mitarbeiter-Account einrichten — " + companyName, user.getMail(), Optional.empty());
    }

    public void sendUserPasswordReset(SsoUser user, final String token) {
        Context context = new Context();
        context.setVariable("firstName", user.getFirstName());
        context.setVariable("lastName", user.getLastName());
        context.setVariable("companyName", companyName);
        context.setVariable("token", token);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        context.setVariable("ssoUrl", baseUrl);
        String htmlBody = templateEngine.process("emails/user-forgot", context);
        send(htmlBody, "Passwort zurück setzen", user.getMail(), Optional.empty());
    }

    record MailAttachment(String name, byte[] content, String contentType){}


    public void send(String body, String subject, String recipient, Optional<MailAttachment> attachmentOpt) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setFrom(sender);
            helper.setText(body, true);

            if (attachmentOpt.isPresent()) {
                MailAttachment attachment = attachmentOpt.get();

                // Falls du Schritt 1 (byte[]) umgesetzt hast:
                byte[] bytes = attachment.content();
                // Falls dein Modell zwingend String geblieben ist, nimm diese Zeile stattdessen:
                // byte[] bytes = attachment.getContent().getBytes(java.nio.charset.StandardCharsets.UTF_8);

                // FIX: Wir verpacken die Bytes in eine offizielle MIME-DataSource mit UTF-8-Deklaration
                ByteArrayDataSource dataSource = new ByteArrayDataSource(bytes, "application/json; charset=UTF-8");

                // Das zwingt das Mail-Framework dazu, den Anhang im standardisierten Base64-Format zu senden
                helper.addAttachment(attachment.name(), dataSource);
            }

            mailSender.send(mimeMessage);
            log.info("E-Mail mit Base64-codiertem UTF-8-Anhang erfolgreich übermittelt.");

        } catch (Exception e) {
            log.error("Fehler beim Erstellen der HTML-OIDC-Mail für {}", recipient, e);
            throw new IllegalStateException("Mail-Versand fehlgeschlagen", e);
        }
    }
    public void sendGdprReportEmail(final String mail, final Map<String, Object> gdprReport) {
        try {

            byte[] jsonBytes = objectMapper.writer()
                .with(tools.jackson.databind.SerializationFeature.INDENT_OUTPUT)
                .writeValueAsBytes(gdprReport);
            // 2. Thymeleaf-Kontext vorbereiten und Variablen setzen
            Context thymeleafContext = new Context();
            thymeleafContext.setVariable("companyName", companyName);

            // 3. Template parsen und in HTML-String transformieren
            // Sucht automatisch nach 'templates/mail/gdpr-export.html'
            String htmlBody = templateEngine.process("emails/gdpr-export", thymeleafContext);

            // 4. Dateiname definieren und senden
            String reportId = gdprReport.getOrDefault("report_id", "sso-export").toString();
            String fileName = "gdpr_export_" + reportId + ".json";
            final MailAttachment attachment = new MailAttachment(fileName, jsonBytes , "application/json");
            send(htmlBody, "Deine angeforderte DSGVO-Selbstauskunft", mail, Optional.of(attachment));

        } catch (Exception e) {
            log.error("Fehler bei der HTML-Generierung über Thymeleaf für {}", mail, e);
            throw new IllegalStateException("DSGVO-Export-Versand fehlgeschlagen", e);
        }
    }

}
