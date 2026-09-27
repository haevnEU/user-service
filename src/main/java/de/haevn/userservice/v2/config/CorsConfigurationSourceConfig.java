package de.haevn.userservice.v2.config;


import de.haevn.userservice.v2.model.SsoApplication;
import de.haevn.userservice.v2.repository.ApplicationRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
@Configuration
@RequiredArgsConstructor
public class CorsConfigurationSourceConfig {
    private final ApplicationRepository applicationRepository;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            String originHeader = request.getHeader("Origin");

            // Wenn kein Origin-Header da ist (Standard-Formular-POSTs ohne CORS),
            // überlassen wir die Prüfung den normalen Security-Filtern (kein CORS-Block)
            if (originHeader == null || originHeader.isBlank()) {
                return null;
            }

            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedMethods(List.of("POST", "GET", "OPTIONS"));
            configuration.setAllowedHeaders(List.of("Content-Type", "Authorization"));
            configuration.setAllowCredentials(true);

            // NEU: Eigene Server-Domain dynamisch ermitteln und immer erlauben (Same-Origin)
            String serverUrl = request.getScheme() + "://" + request.getServerName();
            if (request.getServerPort() != 80 && request.getServerPort() != 443) {
                serverUrl += ":" + request.getServerPort();
            }

            if (originHeader.equals(serverUrl)) {
                configuration.setAllowedOrigins(List.of(originHeader));
                return configuration;
            }

            // Dynamischer Abgleich mit registrierten OIDC-Redirect-URIs aus der DB
            List<SsoApplication> activeApps = applicationRepository.findAll();

            boolean isAllowedOrigin = activeApps.stream()
                .filter(SsoApplication::isActive)
                .flatMap(app -> app.getRedirectUris().stream())
                .anyMatch(uri -> uri.startsWith(originHeader));

            if (isAllowedOrigin) {
                configuration.setAllowedOrigins(List.of(originHeader));
                return configuration; // Sauberer Return
            }

            // Wenn die Origin bekannt, aber nicht erlaubt ist -> explizit sperren
            return null;
        };
    }}
