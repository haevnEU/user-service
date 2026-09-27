package de.haevn.userservice.v2.repository;

import de.haevn.userservice.v2.model.SsoUser;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SsoUserRepository extends JpaRepository<SsoUser, String> {
    @Modifying
    @Query("DELETE FROM SsoUser u WHERE u.offboardingDate IS NOT NULL AND u.offboardingDate < :stichtag")
    int deleteByOffboardingDateBefore(@Param("stichtag") Instant stichtag);

    @Modifying
    @Query("UPDATE SsoUser u SET u.active = false, " +
        "u.firstName = 'Deleted', " +
        "u.lastName = '[ANONYMIZED]', " +
        "u.mail = CONCAT('anonymous.', SUBSTRING(CAST(FUNCTION('gen_random_uuid') AS string), 1, 8), '@internal.local'), " +
        "u.password = CONCAT('{noop}', CAST(FUNCTION('gen_random_uuid') AS string)), " +
        "u.twoFactorSecret = null, " +
        "u.twoFactorEnabled = false, " +
        "u.organizationalUnitId = null " + // <-- HIER u.role entfernt!
        "WHERE u.offboardingDate IS NOT NULL AND u.offboardingDate < :now AND u.lastName != '[ANONYMIZED]'")
    int anonymizeOffboardedUsers(@Param("now") Instant now);
    @Modifying
    @Query(value = "DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE last_name = '[ANONYMIZED]')", nativeQuery = true)
    void clearAnonymizedUserRoles();
    Optional<SsoUser> findByMail(String mail);

    Optional<SsoUser> findByPasswordResetToken(String token);

    List<SsoUser> findAllByOffboardingDateBefore(Instant cutoff);
}
