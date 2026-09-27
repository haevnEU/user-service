package de.haevn.userservice.v2.gdpr;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    /**
     * Findet alle Audit-Logs zu einer bestimmten User-ID, sortiert nach Erstellungsdatum.
     */
    List<AuditLog> findAllByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Findet alle Logs eines bestimmten Event-Typs (z.B. GDPR_PURGE).
     */
    List<AuditLog> findAllByEventType(EventType eventType);

    // 1. Wenn KEIN Suchbegriff eingegeben wurde (verhindert den bytea-Fehler beim Laden der Seite)
    @Query("SELECT a FROM AuditLog a WHERE " +
        "(:requestedBy IS NULL OR a.requestedBy = :requestedBy) " +
        "AND (:eventType IS NULL OR a.eventType = :eventType) " +
        "AND (:status IS NULL OR a.status = :status)")
    List<AuditLog> findByFiltersWithoutSearch(@Param("requestedBy") String requestedBy,
        @Param("eventType") EventType eventType,
        @Param("status") AuditStatus status,
        Sort sort);

    @Query("SELECT a FROM AuditLog a WHERE " +
        "(LOWER(a.requestedBy) LIKE LOWER(CONCAT('%', :search, '%')) OR a.userId = :search) " + // <-- Hier integriert
        "AND (:requestedBy IS NULL OR a.requestedBy = :requestedBy) " +
        "AND (:eventType IS NULL OR a.eventType = :eventType) " +
        "AND (:status IS NULL OR a.status = :status)")
    List<AuditLog> findByFiltersWithSearch(@Param("search") String search,
        @Param("requestedBy") String requestedBy,
        @Param("eventType") EventType eventType,
        @Param("status") AuditStatus status,
        Sort sort);
}