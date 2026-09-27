package de.haevn.userservice.v2.gdpr;

import de.haevn.userservice.v2.model.SsoUser;
import de.haevn.userservice.v2.service.RequestUtils;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuditLogService {
    private final LegalNotice legalNotice;
    private final DataProcessing dataProcessing;
    private final AuditLogRepository auditLogRepository;

    public Map<String, Object> createGDPR(final SsoUser user, UserDetails managerDetails) {
        final String reportId = UUID.randomUUID().toString();
        final Instant now = Instant.now();

        final Map<String, Object> gdpr = new HashMap<>();

        gdpr.put("export_date", DateTimeFormatter.ISO_INSTANT.format(now));
        gdpr.put("export_version", 2);
        gdpr.put("legal_notice", legalNotice);
        gdpr.put("user_data", new UserData(user));
        gdpr.put("data_processing_information", dataProcessing);
        gdpr.put("report_id", reportId);

        AuditLog auditLog = AuditLog.builder()
            .eventType(EventType.GDPR_EXPORT)
            .userId(user.getId())
            .requestedBy(managerDetails.getUsername())
            .status(AuditStatus.SUCCEED)
            .ipAddress(RequestUtils.getClientIp())
            .createdAt(now)
            .build();

        final AuditLog savedAuditLog = auditLogRepository.save(auditLog);

        gdpr.put("export_id", savedAuditLog.getExportId());
        return gdpr;
    }
}