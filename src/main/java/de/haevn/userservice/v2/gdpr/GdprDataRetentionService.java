package de.haevn.userservice.v2.gdpr;

import de.haevn.userservice.v2.model.SsoUser;
import de.haevn.userservice.v2.repository.SsoUserRepository;
import de.haevn.userservice.v2.service.RequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GdprDataRetentionService {

    private final SsoUserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    private static final long RETENTION_YEARS = 3;

    @Scheduled(cron = "0 0 3 * * *") // Täglich um 03:00 Uhr Serverzeit
    @Transactional
    public void cleanupExpiredUsers() {
        log.info("Starte automatischen DSGVO-Cleanup-Prozess...");

        // Berechnung des Cutoff-Zeitpunkts über Instant
        Instant cutoff = Instant.now().minus(RETENTION_YEARS * 365, ChronoUnit.DAYS);
        List<SsoUser> expiredUsers = userRepository.findAllByOffboardingDateBefore(cutoff);

        if (expiredUsers.isEmpty()) {
            log.info("Keine abgelaufenen Benutzerkonten gefunden.");
            return;
        }

        log.info("{} abgelaufene Benutzerkonten zur Anonymisierung gefunden.", expiredUsers.size());

        for (SsoUser user : expiredUsers) {
            try {
                anonymizeUser(user);
            } catch (Exception e) {
                log.error("Fehler bei der Anonymisierung von User-ID: {}", user.getId(), e);
            }
        }
        log.info("DSGVO-Cleanup-Prozess erfolgreich beendet.");
    }

    private void anonymizeUser(SsoUser user) {
        String shortId = UUID.randomUUID().toString().substring(0, 8);
        String placeholder = "[ANONYMIZED_" + shortId + "]";

        log.info("Anonymisiere Benutzerkonto: {} (ID: {})", user.getMail(), user.getId());

        user.setMail(placeholder + "@company-anonymous.local");
        user.setFirstName("Anonymisiert");
        user.setLastName("Mitarbeiter");
        user.setPassword(""); // Vollständig leeren für maximale Datensparsamkeit
        user.setEnabled(false);
        user.setTwoFactorSecret(null);
        user.getRoles().clear(); // Bereinigt die user_roles Verknüpfungstabelle

        userRepository.save(user);

        AuditLog cleanupLog = AuditLog.builder()
            .eventType(EventType.GDPR_PURGE)
            .userId(user.getId())
            .requestedBy("SYSTEM_RETENTION_CRON")
            .status(AuditStatus.SUCCEED)
            .createdAt(Instant.now())
            .ipAddress(RequestUtils.getClientIp())
            .build();

        auditLogRepository.save(cleanupLog);
    }
}