package de.haevn.userservice.v2.gdpr;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(
    name = "sso_gdpr_audit",
    indexes = {
        @Index(name = "idx_gdpr_audit_user_id", columnList = "user_id"),
        @Index(name = "idx_gdpr_audit_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "export_id", updatable = false, nullable = false)
    private String exportId;

    @Column(name = "event_type", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId; // Als String-Mapping passend zum SsoUser

    @Column(name = "requested_by", nullable = false, updatable = false)
    private String requestedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // Instant für garantierte UTC-Zentralisierung

    @Column(name = "status", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private AuditStatus status;

    @Column(name = "ip_address", updatable = false)
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}