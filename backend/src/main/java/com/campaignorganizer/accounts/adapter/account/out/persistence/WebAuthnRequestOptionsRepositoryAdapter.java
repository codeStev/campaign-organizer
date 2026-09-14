package com.campaignorganizer.accounts.adapter.account.out.persistence;

import com.campaignorganizer.security.CurrentUserPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository;
import org.springframework.security.web.webauthn.jackson.WebauthnJackson2Module;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stateless replacement for the session-backed default, for the login-challenge side of a
 * WebAuthn ceremony — see {@link WebAuthnCreationOptionsRepositoryAdapter}'s Javadoc for the
 * full reasoning, identical here except for {@code PURPOSE}.
 *
 * <p>Unlike the registration side, {@code PublicKeyCredentialRequestOptionsFilter} (which calls
 * this repository) carries no authentication check of its own — by the WebAuthn spec's own
 * "usernameless" resident-key login model, the server can't know who's signing in before a
 * credential is picked, so Spring deliberately lets this endpoint through unauthenticated. This
 * app's actual login flow always presents a PASSWORD-factor pending token before reaching this
 * step regardless (same as TOTP's challenge step), but since Spring itself won't enforce that
 * here, {@link #requireAccountId()} does — failing with a clean 401 instead of letting
 * {@link CurrentUserPort#currentAccountId()} crash on an anonymous principal it wasn't built to
 * expect (caught by WebAuthnCeremonyWiringIT calling this endpoint with no bearer token).
 */
@Component
public class WebAuthnRequestOptionsRepositoryAdapter implements PublicKeyCredentialRequestOptionsRepository {

    private static final String PURPOSE = "AUTHENTICATION";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final WebAuthnChallengeJpaRepository repository;
    private final CurrentUserPort currentUser;
    private final Clock clock;
    // See WebAuthnCreationOptionsRepositoryAdapter's Javadoc for why this stays on the
    // deprecated Jackson 2.x module rather than adopting Jackson 3.x for one internal use.
    @SuppressWarnings("removal")
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new WebauthnJackson2Module());

    public WebAuthnRequestOptionsRepositoryAdapter(WebAuthnChallengeJpaRepository repository,
                                                    CurrentUserPort currentUser, Clock clock) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void save(HttpServletRequest request, HttpServletResponse response, PublicKeyCredentialRequestOptions options) {
        // WebAuthnAuthenticationFilter calls save(..., null) to invalidate the pending challenge
        // right after loading it, before the actual assertion verification runs — an explicit
        // delete here (rather than overwriting with a JSON "null" string, which load() happened
        // to also treat as absent) makes that single-use redemption obvious rather than implicit.
        if (options == null) {
            repository.deleteById(requireAccountId());
            return;
        }
        WebAuthnChallengeJpaEntity entity = new WebAuthnChallengeJpaEntity();
        entity.setAccountId(requireAccountId());
        entity.setPurpose(PURPOSE);
        entity.setOptionsJson(writeJson(options));
        entity.setExpiresAt(clock.instant().plus(TTL));
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicKeyCredentialRequestOptions load(HttpServletRequest request) {
        return repository.findById(requireAccountId())
                .filter(entity -> PURPOSE.equals(entity.getPurpose()))
                .filter(entity -> entity.getExpiresAt().isAfter(clock.instant()))
                .map(entity -> readJson(entity.getOptionsJson()))
                .orElse(null);
    }

    private UUID requireAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID accountId)) {
            throw new InsufficientAuthenticationException(
                    "WebAuthn login challenge requires a PASSWORD-factor bearer token");
        }
        return accountId;
    }

    private String writeJson(PublicKeyCredentialRequestOptions options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize WebAuthn request options", ex);
        }
    }

    private PublicKeyCredentialRequestOptions readJson(String json) {
        try {
            return objectMapper.readValue(json, PublicKeyCredentialRequestOptions.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize WebAuthn request options", ex);
        }
    }
}
