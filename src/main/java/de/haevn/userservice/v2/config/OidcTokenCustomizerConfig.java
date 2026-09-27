package de.haevn.userservice.v2.config;

import de.haevn.userservice.v2.model.SsoUser;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class OidcTokenCustomizerConfig {

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            // Wir passen sowohl Access Tokens als auch ID Tokens an
            if (context.getTokenType() != null) {

                // Extrahiert die Principal-Authentisierung des aktuell angemeldeten Benutzers
                Object principal = context.getPrincipal().getPrincipal();

                // Prüft, ob es sich um unseren SsoUser handelt (Form-Login / Session)
                if (principal instanceof SsoUser ssoUser) {

                    // 1. Rollen extrahieren (z.B. ROLE_DEVELOPER, ROLE_ADMIN)
                    Set<String> roles = context.getPrincipal().getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(Objects::nonNull)
                        .filter(role->role.startsWith("ROLE_"))
                        .collect(Collectors.toSet());

                    Set<String> securityFeatures = context.getPrincipal().getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(Objects::nonNull)
                        .filter(auth -> auth.startsWith("FACTOR_"))
                        .map(auth -> auth.replace("FACTOR_", "").toLowerCase())
                        .collect(Collectors.toSet());
                    // 2. Claims in den JWT-Builder schreiben
                    Set<String> authorizedScopes = context.getAuthorizedScopes();
                    if (authorizedScopes.contains("roles")) {
                        context.getClaims().claim("roles", roles);
                    } if (authorizedScopes.contains("profile")) {
                        context.getClaims().claim("name", ssoUser.getFirstName() + " " + ssoUser.getLastName());
                    }

                    if (authorizedScopes.contains("email") && ssoUser.getMail() != null) {
                        context.getClaims().claim("email", ssoUser.getMail());
                    }

                    if (authorizedScopes.contains("organization") && ssoUser.getOrganizationalUnitId() != null) {
                        context.getClaims().claim("organizationUnitId", ssoUser.getOrganizationalUnitId());
                    }

                    if (authorizedScopes.contains("securityFeatures")) {
                        context.getClaims().claim("securityFeatures", securityFeatures);
                    }

                    if (authorizedScopes.contains("accountStatus")) {
                        context.getClaims().claim("accountStatus", ssoUser.getAccountStatus());
                    }
                }
            }
        };
    }
}