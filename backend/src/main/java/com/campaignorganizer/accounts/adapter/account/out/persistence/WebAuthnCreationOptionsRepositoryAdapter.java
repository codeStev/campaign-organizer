package com.campaignorganizer.accounts.adapter.account.out.persistence;

import com.campaignorganizer.security.CurrentUserPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.jackson.WebauthnJackson2Module;
import org.springframework.security.web.webauthn.registration.PublicKeyCredentialCreationOptionsRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stateless replacement for the session-backed default (this app has no {@code HttpSession} at
 * all) — the outstanding registration challenge lives in the {@code webauthn_challenges} table
 * instead, keyed by account id. {@code PublicKeyCredentialCreationOptionsFilter} (which calls
 * this repository) carries its own internal "any authenticated principal" check and writes 400
 * itself before ever reaching {@link #save}/{@link #load} otherwise, so {@link CurrentUserPort}
 * already has a real account id from the security context by the time either method runs here —
 * unlike {@link WebAuthnRequestOptionsRepositoryAdapter}, whose filter has no such check.
 */
@Component
public class WebAuthnCreationOptionsRepositoryAdapter implements PublicKeyCredentialCreationOptionsRepository {

    private static final String PURPOSE = "REGISTRATION";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final WebAuthnChallengeJpaRepository repository;
    private final CurrentUserPort currentUser;
    private final Clock clock;
    // WebauthnJackson2Module is deprecated for removal in favor of a newer Jackson 3.x
    // ("tools.jackson") module — but this whole app is Jackson 2.x (com.fasterxml.jackson)
    // throughout, including its autoconfigured MVC ObjectMapper, so adopting Jackson 3.x here
    // for one internal persistence use would add a second Jackson stack for no real benefit.
    // Revisit once Spring Security actually removes the classic module.
    @SuppressWarnings("removal")
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new WebauthnJackson2Module());

    public WebAuthnCreationOptionsRepositoryAdapter(WebAuthnChallengeJpaRepository repository,
                                                     CurrentUserPort currentUser, Clock clock) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void save(HttpServletRequest request, HttpServletResponse response, PublicKeyCredentialCreationOptions options) {
        // WebAuthnRegistrationFilter calls save(..., null) to invalidate the pending challenge
        // right after loading it, before the actual credential verification runs — an explicit
        // delete here (rather than overwriting with a JSON "null" string, which load() happened
        // to also treat as absent) makes that single-use redemption obvious rather than implicit.
        if (options == null) {
            repository.deleteById(currentUser.currentAccountId());
            return;
        }
        WebAuthnChallengeJpaEntity entity = new WebAuthnChallengeJpaEntity();
        entity.setAccountId(currentUser.currentAccountId());
        entity.setPurpose(PURPOSE);
        entity.setOptionsJson(writeJson(options));
        entity.setExpiresAt(clock.instant().plus(TTL));
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicKeyCredentialCreationOptions load(HttpServletRequest request) {
        return repository.findById(currentUser.currentAccountId())
                .filter(entity -> PURPOSE.equals(entity.getPurpose()))
                .filter(entity -> entity.getExpiresAt().isAfter(clock.instant()))
                .map(entity -> readJson(entity.getOptionsJson()))
                .orElse(null);
    }

    private String writeJson(PublicKeyCredentialCreationOptions options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize WebAuthn creation options", ex);
        }
    }

    private PublicKeyCredentialCreationOptions readJson(String json) {
        try {
            return objectMapper.readValue(json, PublicKeyCredentialCreationOptions.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize WebAuthn creation options", ex);
        }
    }
}
