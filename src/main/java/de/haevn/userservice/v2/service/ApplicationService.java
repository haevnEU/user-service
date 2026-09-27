package de.haevn.userservice.v2.service;

import de.haevn.userservice.v2.model.SsoApplication;
import de.haevn.userservice.v2.repository.ApplicationRepository;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public List<SsoApplication> findAll() {
        return applicationRepository.findAll();
    }

    public void deleteById(final String id) {
        final SsoApplication ssoApplication = applicationRepository.findById(id).orElse(null);
        applicationRepository.deleteById(id);
        mailService.sendApplicationRemoved(ssoApplication);
    }

    public void save(final String name, final String maintainer, final String redirectUrisRaw,
        final String postLogoutRedirectUrisRaw, @NonNull final Set<String> scopes, final Set<String> grantTypes) {
        final SsoApplication app = new SsoApplication();
        app.setName(name);
        app.setMaintainer(maintainer);
        app.setClientId(UUID.randomUUID().toString());

        final byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        final String rawSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        app.setClientSecret(passwordEncoder.encode(rawSecret));

        if (redirectUrisRaw != null && !redirectUrisRaw.isBlank()) {
            final Set<String> uris =
                Arrays.stream(redirectUrisRaw.split(",")).map(String::trim).filter(uri -> !uri.isEmpty())
                    .collect(Collectors.toSet());
            app.setRedirectUris(uris);
        }

        if (postLogoutRedirectUrisRaw != null && !postLogoutRedirectUrisRaw.isBlank()) {
            final Set<String> logoutUris =
                Arrays.stream(postLogoutRedirectUrisRaw.split(",")).map(String::trim).filter(uri -> !uri.isEmpty())
                    .collect(Collectors.toSet());
            app.setPostLogoutRedirectUris(logoutUris);
        }

        if(scopes.isEmpty() || !scopes.contains("openid")){
            scopes.add("openid");
        }
        app.setScopes(scopes);
        app.setAuthorizationGrantTypes(grantTypes != null ? grantTypes : Set.of("authorization_code"));


        applicationRepository.save(app);
        app.setClientSecret(rawSecret);
        mailService.sendApplicationRegistered(app);
    }
}
