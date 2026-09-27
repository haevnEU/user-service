package de.haevn.userservice.v2.repository;

import de.haevn.userservice.v2.model.SsoApplication;
import de.haevn.userservice.v2.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class JpaRegisteredClientRepository implements RegisteredClientRepository {

    private final ApplicationRepository applicationRepository;

    @Override
    public void save(RegisteredClient registeredClient) {
        throw new UnsupportedOperationException("Das Anlegen von Clients erfolgt über die UI-Verwaltung.");
    }

    @Override
    public RegisteredClient findById(String id) {
        return applicationRepository.findById(id)
            .map(this::toRegisteredClient)
            .orElseThrow();
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return applicationRepository.findByClientId(clientId)
            .map(this::toRegisteredClient)
            .orElseThrow();
    }

    private RegisteredClient toRegisteredClient(SsoApplication app) {
        // Nutzt die UUID aus deiner Entität als primären Bezeichner
        RegisteredClient.Builder builder = RegisteredClient.withId(app.getId())
            .clientId(app.getClientId())
            // In Spring Security 7/OAuth2 müssen Secrets in der DB mit dem entsprechenden Prefix
            // (z.B. {bcrypt}...) oder via NoOp hinterlegt sein, passend zu deiner PasswordConfig
            .clientSecret(app.getClientSecret())
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE);

        // Korrigiertes Mapping auf dein Feld: authorizationGrantTypes
        if (app.getAuthorizationGrantTypes() != null) {
            app.getAuthorizationGrantTypes().forEach(gt ->
                builder.authorizationGrantType(new AuthorizationGrantType(gt))
            );
        }

        // Redirect URIs mappen
        if (app.getRedirectUris() != null) {
            app.getRedirectUris().forEach(builder::redirectUri);
        }

        // Post Logout Redirect URIs mappen
        if (app.getPostLogoutRedirectUris() != null) {
            app.getPostLogoutRedirectUris().forEach(builder::postLogoutRedirectUri);
        }

        // Scopes mappen (Falls leer, Standard-OIDC Scopes als Fallback)
        if (app.getScopes() != null && !app.getScopes().isEmpty()) {
            app.getScopes().forEach(builder::scope);
        } else {
            builder.scope(OidcScopes.OPENID).scope(OidcScopes.PROFILE).scope(OidcScopes.EMAIL);
        }

        // Injiziert die Time-to-Live Werte aus deinen JPA-Spalten
        builder.tokenSettings(TokenSettings.builder()
            .accessTokenTimeToLive(Duration.ofSeconds(app.getAccessTokenTimeToLiveSeconds()))
            .refreshTokenTimeToLive(Duration.ofSeconds(app.getRefreshTokenTimeToLiveSeconds()))
            .build());

        // Sicherheits-Standardkonfiguration für moderne Clients (PKCE erzwingen)
        builder.clientSettings(ClientSettings.builder()
            .requireProofKey(true)
            .requireAuthorizationConsent(false) // Keine lästige Zwischenseite für interne Apps
            .build());

        return builder.build();
    }
}