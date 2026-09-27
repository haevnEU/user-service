package de.haevn.userservice.v2.gdpr;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.haevn.userservice.v2.model.SsoUser;
import java.time.Instant;
import java.util.Collection;
import lombok.Data;

@Data
public final class UserData {

    @JsonProperty("unique_user_id")
    private final String id;

    @JsonProperty("first_name")
    private final String firstName;

    @JsonProperty("last_name")
    private final String lastName;

    private final String email;

    @JsonProperty("organizational_unit_id")
    private final String organizationalUnitId;

    private final Collection<String> roles;
    private final String password;

    @JsonProperty("two_factor_enabled")
    private final boolean twoFactorEnabled;

    @JsonProperty("two_factor_secret")
    private final String twoFactorSecret;

    @JsonProperty("account_active")
    private final boolean accountActive;

    @JsonProperty("account_status")
    private final String accountStatus;

    @JsonProperty("failed_login_attempts")
    private final int failedLoginAttempts;

    @JsonProperty("account_locked_until")
    private final Instant accountLockedUntil;

    @JsonProperty("registration_date")
    private final Instant registrationDate;

    @JsonProperty("last_login_date")
    private final Instant lastLoginDate;

    @JsonProperty("offboarding_date")
    private final Instant offboardingDate;

    public UserData(SsoUser user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getMail();
        this.organizationalUnitId = user.getOrganizationalUnitId();
        this.roles = user.getRoles();
        this.password = user.getPassword() != null ? "[REDACTED]" : "";
        this.twoFactorEnabled = user.isTwoFactorEnabled();

        // Sicherheitsrelevantes Secret maskieren, falls vorhanden
        this.twoFactorSecret = user.getTwoFactorSecret() != null ? "[PROTECTED_SECRET]" : null;

        this.accountActive = user.isActive();
        this.accountStatus = user.getAccountStatus(); // Nutzt deine dynamische Methode aus SsoUser
        this.failedLoginAttempts = user.getFailedLoginAttempts();
        this.accountLockedUntil = user.getAccountLockedUntil();

        // Zeitstempel
        this.registrationDate = user.getRegistrationDate();
        this.lastLoginDate = user.getLastLoginDate();
        this.offboardingDate = user.getOffboardingDate();

    }
}