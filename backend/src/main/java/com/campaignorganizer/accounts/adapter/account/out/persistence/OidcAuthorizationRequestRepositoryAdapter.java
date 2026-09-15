package com.campaignorganizer.accounts.adapter.account.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stateless replacement for {@code oauth2Login()}'s default
 * {@code HttpSessionOAuth2AuthorizationRequestRepository} — this app has no {@code HttpSession}
 * at all (ADR-0113). Unlike {@link WebAuthnCreationOptionsRepositoryAdapter} (keyed by
 * account id, since every WebAuthn ceremony already has a PASSWORD-factor token), an initial
 * Google login has no account or token yet — the OAuth {@code state} parameter is the only
 * available correlation key, so it's the primary key here.
 */
@Component
public class OidcAuthorizationRequestRepositoryAdapter implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final OidcAuthorizationRequestJpaRepository repository;
    private final Clock clock;
    // Deprecated for removal in favor of a newer Jackson 3.x ("tools.jackson") module — same
    // situation as WebAuthnCreationOptionsRepositoryAdapter's WebauthnJackson2Module: this whole
    // app is Jackson 2.x throughout, so adopting Jackson 3.x here for one internal persistence
    // use would add a second Jackson stack for no real benefit. Revisit once Spring Security
    // actually removes the classic module.
    //
    // SecurityJackson2Modules.getModules(...) is also required, not just OAuth2ClientJackson2Module
    // alone — OAuth2AuthorizationRequest.getScopes() is an unmodifiable Set at runtime
    // (java.util.Collections$UnmodifiableSet), and Spring Security's own allowlisting
    // deserializer rejects any concrete type it doesn't already have a mixin for; the core
    // modules register exactly those mixins for common JDK collection types.
    @SuppressWarnings("removal")
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()))
            .registerModule(new OAuth2ClientJackson2Module());

    public OidcAuthorizationRequestRepositoryAdapter(OidcAuthorizationRequestJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        if (state == null) {
            return null;
        }
        return repository.findById(state)
                .filter(entity -> entity.getExpiresAt().isAfter(clock.instant()))
                .map(entity -> readJson(entity.getRequestJson()))
                .orElse(null);
    }

    @Override
    @Transactional
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }
        OidcAuthorizationRequestJpaEntity entity = new OidcAuthorizationRequestJpaEntity();
        entity.setState(authorizationRequest.getState());
        entity.setRequestJson(writeJson(authorizationRequest));
        entity.setExpiresAt(clock.instant().plus(TTL));
        repository.save(entity);
    }

    @Override
    @Transactional
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        if (state == null) {
            return null;
        }
        return repository.findById(state)
                .map(entity -> {
                    repository.deleteById(state);
                    return readJson(entity.getRequestJson());
                })
                .orElse(null);
    }

    private String writeJson(OAuth2AuthorizationRequest authorizationRequest) {
        try {
            return objectMapper.writeValueAsString(authorizationRequest);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize OAuth2AuthorizationRequest", ex);
        }
    }

    private OAuth2AuthorizationRequest readJson(String json) {
        try {
            return objectMapper.readValue(json, OAuth2AuthorizationRequest.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize OAuth2AuthorizationRequest", ex);
        }
    }
}
