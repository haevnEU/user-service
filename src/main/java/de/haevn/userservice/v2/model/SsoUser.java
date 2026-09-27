package de.haevn.userservice.v2.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import static jakarta.persistence.FetchType.*;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@Setter
@Entity
@Table(name = "users")
public class SsoUser implements Persistable<String>, UserDetails {
    @Id
    String id;
    String firstName;
    String lastName;
    String mail;
    String organizationalUnitId;
    String password;
    @ElementCollection(fetch = EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    private List<String> roles;    Instant registrationDate;
    Instant offboardingDate;
    Instant lastLoginDate;
    boolean twoFactorEnabled = false;
    String twoFactorSecret;
    boolean active = true;
    @Transient
    private boolean isNew = true;
    int failedLoginAttempts = 0;
    Instant accountLockedUntil;

    // Neue Spalten für Passwort-Reset (Token-basiert)
    String passwordResetToken;
    Instant passwordResetTokenExpiry;
    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(SimpleGrantedAuthority::new).toList();
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        // Für den Login via SSO ist die E-Mail-Adresse meistens der Username
        return this.mail;
    }

    @Transient // Wird nicht in der DB gespeichert, da dynamisch berechnet
    public String getAccountStatus() {
        if (!this.active) {
            return "SUSPENDED";
        }

        Instant now = Instant.now();

        // Fall A: Mitarbeiter hat das Unternehmen bereits verlassen
        if (this.offboardingDate != null && now.isAfter(this.offboardingDate)) {
            return "OFFBOARDED";
        }

        // Fall B: Mitarbeiter ist registriert, aber sein erster Arbeitstag liegt in der Zukunft
        if (this.registrationDate != null && now.isBefore(this.registrationDate)) {
            return "PENDING";
        }

        return "ACTIVE";
    }
    @Override
    public boolean isAccountNonExpired() {
        String status = getAccountStatus();
        // PENDING und OFFBOARDED Konten dürfen sich nicht einloggen
        return !"OFFBOARDED".equals(status) && !"PENDING".equals(status);
    }

    @Override
    public boolean isAccountNonLocked() {
        // 1. Wenn manuell gesperrt (active = false / SUSPENDED)
        if (!active) {
            return false;
        }

        // 2. Wenn eine temporäre Brute-Force-Sperre aktiv ist
        if (accountLockedUntil != null && Instant.now().isBefore(accountLockedUntil)) {
            return false;
        }
        return !"SUSPENDED".equals(getAccountStatus());
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(getAccountStatus());
    }
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    public void setEnabled(boolean active) {
        this.active = active;
    }
}
