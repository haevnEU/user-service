package de.haevn.userservice.v2.cron;

import de.haevn.userservice.v2.repository.SsoUserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Component
@RequiredArgsConstructor
public class OffboardingScheduler {
    private final SsoUserRepository userRepository;

    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void processDailyOffboardings() {
        log.info("Starte performanten Offboarding- und Anonymisierungs-Check...");

        int anonymizedCount = userRepository.anonymizeOffboardedUsers(Instant.now());
        if (anonymizedCount > 0) {
            log.info("{} ausgetretene Mitarbeiterkonten wurden erfolgreich anonymisiert und deaktiviert.",
                anonymizedCount);
        } else {
            log.info("Keine anstehenden Offboardings für heute zu verarbeiten.");
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void hardDeleteOldOffboardedUsersEfficiently() {
        log.info("Starte performanten Bereinigungs-Check (3 Jahre)...");

        final Instant threeYearsAgo = Instant.now().minus(3 * 365, java.time.temporal.ChronoUnit.DAYS);
        final int deletedCount = userRepository.deleteByOffboardingDateBefore(threeYearsAgo);

        if (deletedCount > 0) {
            log.info("{} alte Benutzerkonten wurden erfolgreich aus der Datenbank gelöscht.", deletedCount);
        }
    }
}
