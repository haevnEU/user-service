package de.haevn.userservice.v2.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(of = {"id", "name", "clientId", "maintainer"})
@Entity
@Table(name = "applications")
public class SsoApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String maintainer;

    @Column(nullable = false, updatable = false)
    private Instant creationDate = Instant.now();

    @Column(nullable = false)
    private boolean active = true;


    @Column(nullable = false, unique = true)
    private String clientId;

    @Column(nullable = false)
    private String clientSecret;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "application_redirect_uris", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "redirect_uri", length = 1000)
    private Set<String> redirectUris;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "application_post_logout_redirect_uris", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "post_logout_redirect_uri", length = 1000)
    private Set<String> postLogoutRedirectUris;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "application_scopes", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "scope")
    private Set<String> scopes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "application_grant_types", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "grant_type")
    private Set<String> authorizationGrantTypes;

    @Column(nullable = false)
    private long accessTokenTimeToLiveSeconds = 300;

    @Column(nullable = false)
    private long refreshTokenTimeToLiveSeconds = 86400;
}