package de.haevn.userservice.v2.hrworks;

import de.haevn.userservice.v2.gdpr.AuditLog;
import de.haevn.userservice.v2.gdpr.AuditLogRepository;
import de.haevn.userservice.v2.gdpr.AuditStatus;
import de.haevn.userservice.v2.gdpr.EventType;
import de.haevn.userservice.v2.model.SsoUser;
import de.haevn.userservice.v2.repository.SsoUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Log4j2
@RequiredArgsConstructor
@Controller
@RequestMapping("/users")
public class UserController {
    private final SsoUserRepository userRepository;
    private final AuditLogRepository  auditLogRepository;

    @GetMapping
    public String listUsers(Model model) {
        List<SsoUser> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "users/user-list";
    }

    @PostMapping("/update-roles")
    public String updateSingleUserRoles(
        @RequestParam(value = "userId", required = false) String userId,
        HttpServletRequest request,
        RedirectAttributes redirectAttributes) { // <-- NEU

        if (userId == null || userId.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Benutzer-ID fehlt!");
            return "redirect:/users";
        }

        String[] selectedRoles = request.getParameterValues("roles_" + userId);
        List<String> rolesList = (selectedRoles != null) ? List.of(selectedRoles) : List.of();

        userRepository.findById(userId).ifPresent(user -> {
            user.setRoles(rolesList);
            userRepository.save(user);

            // NEU: Vorname und Nachname für die Erfolgsmeldung nutzen
            String fullName = user.getFirstName() + " " + user.getLastName();
            redirectAttributes.addFlashAttribute("successMessage", "Rollen für " + fullName + " erfolgreich aktualisiert.");
        });

        return "redirect:/users";
    }

    @GetMapping("/audit-log")
    public String showGdprAuditLogs(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "requestedBy", required = false) String requestedBy,
        @RequestParam(value = "eventType", required = false) EventType eventType,
        @RequestParam(value = "status", required = false) AuditStatus status,
        @RequestParam(value = "sort", defaultValue = "desc") String sortDir,
        Model model) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ?
            Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();

        // Parameter für die Query säubern
        String cleanedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanedRequestedBy = (requestedBy != null && !requestedBy.isBlank()) ? requestedBy : null;

        List<AuditLog> auditLogs;

        if (cleanedSearch == null) {
            // Sicherer Pfad beim Laden der Seite oder bei reiner Dropdown-Filterung
            auditLogs = auditLogRepository.findByFiltersWithoutSearch(
                cleanedRequestedBy, eventType, status, sort
            );
        } else {
            // Pfad, wenn der Admin aktiv im Suchfeld tippt
            auditLogs = auditLogRepository.findByFiltersWithSearch(
                cleanedSearch, cleanedRequestedBy, eventType, status, sort
            );
        }
        // Liste aller jemals aktiven Initiatoren für das Dropdown-Menü ermitteln
        List<String> distinctRequestedBy = auditLogRepository.findAll().stream()
            .map(AuditLog::getRequestedBy)
            .distinct()
            .toList();

        model.addAttribute("auditLogs", auditLogs);
        model.addAttribute("distinctRequestedBy", distinctRequestedBy);
        model.addAttribute("eventTypes", EventType.values());
        model.addAttribute("auditStatuses", AuditStatus.values());

        // Zustandserhalt im UI
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentRequestedBy", requestedBy);
        model.addAttribute("currentEventType", eventType != null ? eventType.name() : "");
        model.addAttribute("currentStatus", status != null ? status.name() : "");
        model.addAttribute("currentSort", sortDir);

        return "user/gdpr-audit-log";
    }

    @GetMapping("/audit-log/download")
    public void downloadGdprAuditLogsCsv(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String requestedBy,
        @RequestParam(required = false) EventType eventType,
        @RequestParam(required = false) AuditStatus status,
        @Param("sort") Sort sort,
        HttpServletResponse response) throws IOException {

        // 1. Dieselbe Filter-Logik wie für die Tabellenansicht anwenden
        String cleanedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanedRequestedBy = (requestedBy != null && !requestedBy.isBlank()) ? requestedBy : null;

        if (sort == null) {
            sort = Sort.by("createdAt").descending();
        }

        List<AuditLog> auditLogs;
        if (cleanedSearch == null) {
            auditLogs = auditLogRepository.findByFiltersWithoutSearch(
                cleanedRequestedBy, eventType, status, sort
            );
        } else {
            auditLogs = auditLogRepository.findByFiltersWithSearch(
                cleanedSearch, cleanedRequestedBy, eventType, status, sort
            );
        }

        // 2. HTTP-Header für den Datei-Download setzen
        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=\"gdpr_audit_log_export.csv\"");

        // 3. CSV schreiben (mit Semikolon als Delimiter)
        try (PrintWriter writer = response.getWriter()) {
            // Optional: Byte Order Mark (BOM) für Excel hinzufügen, damit Umlaute direkt sauber erkannt werden
            writer.write('\ufeff');

            // CSV-Header-Zeile
            writer.println("Export-ID;Zeitpunkt;Request-IP;Initiator (Requested By);Betroffener User (User ID);Event-Typ;Status");


            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(java.time.ZoneOffset.UTC); // <-- Das ist der Schlüssel!

            // 2. In der Schleife rufst du es dann umgekehrt auf:
            // Datenzeilen schreiben
            for (AuditLog log : auditLogs) {
                String requestIp = log.getIpAddress() != null ? log.getIpAddress() : "";
                String createdAt = log.getCreatedAt() != null ? formatter.format(log.getCreatedAt()) : "";
                String exportId = log.getExportId() != null ? log.getExportId() : "";
                String reqBy = log.getRequestedBy() != null ? log.getRequestedBy() : "";
                String uId = log.getUserId() != null ? log.getUserId() : "";
                String eType = log.getEventType() != null ? log.getEventType().name() : "";
                String aStatus = log.getStatus() != null ? log.getStatus().name() : "";

                // Werte zusammensetzen und escapen, falls Felder Semikolons enthalten könnten
                writer.printf("%s;%s;%s;%s;%s;%s;%s%n",
                    escapeCsv(exportId),
                    escapeCsv(createdAt),
                    escapeCsv(requestIp),
                    escapeCsv(reqBy),
                    escapeCsv(uId),
                    escapeCsv(eType),
                    escapeCsv(aStatus)
                );
            }
            writer.flush();
        }
    }

    // Kleine Hilfsmethode, um das Brechen der CSV-Struktur durch enthaltene Semikolons oder Newlines zu verhindern
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
