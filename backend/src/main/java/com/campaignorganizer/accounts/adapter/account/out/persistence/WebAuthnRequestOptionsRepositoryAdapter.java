package com.campaignorganizer.accounts.adapter.account.out.persistence;

import com.campaignorganizer.security.CurrentUserPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialDescriptor;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stateless replacement for the session-backed default, for the login-challenge side of a
 * WebAuthn ceremony — see {@link WebAuthnCreationOptionsRepositoryAdapter}'s Javadoc for why
 * this doesn't deserialize {@link PublicKeyCredentialRequestOptions} via Jackson at all (Spring
 * Security's own Jackson support for these {@code *Options} types is serialize-only) and instead
 * rebuilds it from a plain {@link StoredRequestOptions} record via the public builder API.
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
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        entity.setOptionsJson(writeJson(StoredRequestOptions.from(options)));
        entity.setExpiresAt(clock.instant().plus(TTL));
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicKeyCredentialRequestOptions load(HttpServletRequest request) {
        return repository.findById(requireAccountId())
                .filter(entity -> PURPOSE.equals(entity.getPurpose()))
                .filter(entity -> entity.getExpiresAt().isAfter(clock.instant()))
                .map(entity -> readJson(entity.getOptionsJson()).toOptions())
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

    private String writeJson(StoredRequestOptions stored) {
        try {
            return objectMapper.writeValueAsString(stored);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize WebAuthn request options", ex);
        }
    }

    private StoredRequestOptions readJson(String json) {
        try {
            return objectMapper.readValue(json, StoredRequestOptions.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize WebAuthn request options", ex);
        }
    }

    /** Every field here is a plain byte[]/String/Long/List — no Spring Security type, no special Jackson module needed. */
    private record StoredRequestOptions(byte[] challenge, Long timeoutMillis, String rpId,
                                        List<StoredDescriptor> allowCredentials, String userVerification) {

        static StoredRequestOptions from(PublicKeyCredentialRequestOptions options) {
            Duration timeout = options.getTimeout();
            List<PublicKeyCredentialDescriptor> allow = options.getAllowCredentials();
            return new StoredRequestOptions(
                    options.getChallenge().getBytes(),
                    timeout == null ? null : timeout.toMillis(),
                    options.getRpId(),
                    allow == null ? List.of() : allow.stream().map(StoredDescriptor::from).toList(),
                    options.getUserVerification() == null ? null : options.getUserVerification().getValue());
        }

        PublicKeyCredentialRequestOptions toOptions() {
            var builder = PublicKeyCredentialRequestOptions.builder()
                    .challenge(new Bytes(challenge))
                    .rpId(rpId)
                    .allowCredentials(allowCredentials.stream().map(StoredDescriptor::toDescriptor).toList());
            if (timeoutMillis != null) {
                builder.timeout(Duration.ofMillis(timeoutMillis));
            }
            if (userVerification != null) {
                builder.userVerification(
                        WebAuthnCreationOptionsRepositoryAdapter.userVerificationRequirement(userVerification));
            }
            return builder.build();
        }
    }

    private record StoredDescriptor(byte[] id, List<String> transports) {

        static StoredDescriptor from(PublicKeyCredentialDescriptor descriptor) {
            java.util.Set<AuthenticatorTransport> transports = descriptor.getTransports();
            return new StoredDescriptor(descriptor.getId().getBytes(),
                    transports == null ? List.of() : transports.stream().map(AuthenticatorTransport::getValue).toList());
        }

        PublicKeyCredentialDescriptor toDescriptor() {
            return PublicKeyCredentialDescriptor.builder()
                    .type(PublicKeyCredentialType.PUBLIC_KEY)
                    .id(new Bytes(id))
                    .transports(transports.stream().map(StoredDescriptor::authenticatorTransport)
                            .collect(java.util.stream.Collectors.toSet()))
                    .build();
        }

        private static AuthenticatorTransport authenticatorTransport(String value) {
            for (AuthenticatorTransport candidate : AuthenticatorTransport.values()) {
                if (candidate.getValue().equals(value)) {
                    return candidate;
                }
            }
            throw new IllegalStateException("Unknown authenticator transport: " + value);
        }
    }
}
