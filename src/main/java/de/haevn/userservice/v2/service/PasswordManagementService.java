package de.haevn.userservice.v2.service;

import de.haevn.userservice.v2.model.SsoUser;
import de.haevn.userservice.v2.repository.SsoUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordManagementService {

    private final SsoUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String createPasswordResetToken(String mail) {
        SsoUser user = userRepository.findByMail(mail)
            .orElseThrow(() -> new IllegalArgumentException("User nicht gefunden"));

        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        // Token ist exakt 2 Stunden gültig
        user.setPasswordResetTokenExpiry(Instant.now().plus(2, ChronoUnit.HOURS));

        userRepository.save(user);
        return token; // Diesen Token schickst du per Mail-Service an den User
    }

    @Transactional
    public void resetPassword(String token, String newRawPassword) {
        SsoUser user = userRepository.findByPasswordResetToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Ungültiges oder abgelaufenes Token"));

        if (user.getPasswordResetTokenExpiry().isBefore(Instant.now())) {
            throw new IllegalStateException("Token ist abgelaufen");
        }

        // Passwort hashen (BCrypt/Argon2 je nach deinem PasswordEncoder Bean)
        user.setPassword(passwordEncoder.encode(newRawPassword));

        // Token entwerten und fehlerhafte Versuche nach erfolgreichem Reset nullen
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);

        userRepository.save(user);
    }


    public boolean isTokenExpired(final SsoUser user) {
        if (user == null || user.getPasswordResetTokenExpiry() == null) {
            return true;
        }

        return Instant.now().isAfter(user.getPasswordResetTokenExpiry());
    }
}