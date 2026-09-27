package de.haevn.userservice.v2.service;

import de.haevn.userservice.v2.model.SsoUser;
import de.haevn.userservice.v2.repository.SsoUserRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final SsoUserRepository userRepository;
    private final MailService mailService;
    private final PasswordManagementService  passwordManagementService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByMail(username)
            .orElseThrow(() -> new UsernameNotFoundException("Benutzer mit E-Mail " + username + " nicht gefunden"));
    }

    public Optional<SsoUser> findByPersonId(String personId) {
        return userRepository.findById(personId);
    }

    public void create(SsoUser ssoUser) {
        final var userOpt = userRepository.findByMail(ssoUser.getMail());
        if (userOpt.isPresent()) {
            throw new RuntimeException("User already exists");
        }
        userRepository.save(ssoUser);
        String token = passwordManagementService.createPasswordResetToken(ssoUser.getMail());
        mailService.sendUserWelcome(ssoUser, token);
    }

    @Transactional
    public void terminateEmployee(String personId) {
        final SsoUser existingUser = userRepository.findById(personId).orElse(null);
        final Instant now = Instant.now();

        existingUser.setNew(false);

        if (existingUser.getOffboardingDate() != null && existingUser.getOffboardingDate().isBefore(now)) {
            existingUser.setEnabled(false);
            log.warn("User {} wurde sofort deaktiviert, da das Offboarding-Datum in der Vergangenheit liegt.",
                existingUser.getMail());
        } else {
            log.info("Offboarding-Datum für {} auf den {} gesetzt. Account bleibt bis dahin aktiv.",
                existingUser.getMail(), existingUser.getOffboardingDate());
            final Instant offboardingPlus3Months = now.atZone(ZoneOffset.UTC).plusMonths(3).toInstant();

            existingUser.setOffboardingDate(offboardingPlus3Months);
        }

        userRepository.save(existingUser);
    }

    @Transactional
    public void update(@NonNull final SsoUser incomingUser) {
        final SsoUser existingUser = userRepository.findById(incomingUser.getId()).orElseThrow();
        existingUser.setNew(false);
        existingUser.setFirstName(incomingUser.getFirstName());
        existingUser.setLastName(incomingUser.getLastName());
        existingUser.setMail(incomingUser.getMail());
        existingUser.setRegistrationDate(incomingUser.getRegistrationDate());
        existingUser.setOrganizationalUnitId(incomingUser.getOrganizationalUnitId());
        userRepository.save(existingUser);
    }

    public Optional<SsoUser> findByMail(final String mail) {
        return userRepository.findByMail(mail);
    }
}
