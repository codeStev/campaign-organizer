package com.campaignorganizer.accounts.adapter.account.out.persistence;

import com.campaignorganizer.security.CurrentUserPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.security.web.webauthn.api.AttestationConveyancePreference;
import org.springframework.security.web.webauthn.api.AuthenticatorAttachment;
import org.springframework.security.web.webauthn.api.AuthenticatorSelectionCriteria;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialDescriptor;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialParameters;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.ResidentKeyRequirement;
import org.springframework.security.web.webauthn.api.UserVerificationRequirement;
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
 *
 * <p><b>Does not deserialize {@link PublicKeyCredentialCreationOptions} via Jackson at all</b> —
 * found live, in production, the hard way: Spring Security's own {@code
 * PublicKeyCredentialCreationOptionsJackson2Mixin} is serialize-only (it exists to write the
 * options into the HTTP response sent to the browser, which is the only round trip Spring's own
 * reference implementation ever needs — its default session-backed repository just keeps the raw
 * Java object, no JSON involved). {@code PublicKeyCredentialCreationOptions} itself has no public
 * constructor Jackson can use, so a plain {@code readValue(json, PublicKeyCredentialCreationOptions.class)}
 * throws {@code InvalidDefinitionException: no Creators} regardless of which Jackson modules are
 * registered — confirmed by inspecting the mixin directly (only a {@code @JsonInclude} +
 * {@code timeout} serializer, no {@code @JsonCreator}/builder annotation of any kind).
 * {@code WebAuthnCeremonyWiringIT} never caught this: it only asserts on the raw HTTP options
 * response, never that this adapter can read back what it just wrote — the actual save-then-load
 * round trip the real credential-write step depends on.
 *
 * <p>Fixed by not asking Jackson to reconstruct the domain type at all: {@link #save} extracts
 * just the primitive fields ({@link StoredCreationOptions}, a plain record with no Spring
 * Security types in it) and serializes those with an ordinary {@code ObjectMapper} (no special
 * modules needed — bytes/strings/longs only); {@link #load} rebuilds the exact same {@link
 * PublicKeyCredentialCreationOptions} by calling its own public builder directly.
 */
@Component
public class WebAuthnCreationOptionsRepositoryAdapter implements PublicKeyCredentialCreationOptionsRepository {

    private static final String PURPOSE = "REGISTRATION";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final WebAuthnChallengeJpaRepository repository;
    private final CurrentUserPort currentUser;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        entity.setOptionsJson(writeJson(StoredCreationOptions.from(options)));
        entity.setExpiresAt(clock.instant().plus(TTL));
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicKeyCredentialCreationOptions load(HttpServletRequest request) {
        return repository.findById(currentUser.currentAccountId())
                .filter(entity -> PURPOSE.equals(entity.getPurpose()))
                .filter(entity -> entity.getExpiresAt().isAfter(clock.instant()))
                .map(entity -> readJson(entity.getOptionsJson()).toOptions())
                .orElse(null);
    }

    private String writeJson(StoredCreationOptions stored) {
        try {
            return objectMapper.writeValueAsString(stored);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize WebAuthn creation options", ex);
        }
    }

    private StoredCreationOptions readJson(String json) {
        try {
            return objectMapper.readValue(json, StoredCreationOptions.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize WebAuthn creation options", ex);
        }
    }

    /** Every field here is a plain byte[]/String/Long/List — no Spring Security type, no special Jackson module needed. */
    private record StoredCreationOptions(String rpId, String rpName, byte[] userId, String userName,
                                         String userDisplayName, byte[] challenge, List<Long> pubKeyCredParamAlgs,
                                         Long timeoutMillis, List<StoredDescriptor> excludeCredentials,
                                         String authenticatorAttachment, String residentKey, String userVerification,
                                         String attestation) {

        static StoredCreationOptions from(PublicKeyCredentialCreationOptions options) {
            AuthenticatorSelectionCriteria selection = options.getAuthenticatorSelection();
            Duration timeout = options.getTimeout();
            List<PublicKeyCredentialDescriptor> exclude = options.getExcludeCredentials();
            return new StoredCreationOptions(
                    options.getRp().getId(), options.getRp().getName(),
                    options.getUser().getId().getBytes(), options.getUser().getName(), options.getUser().getDisplayName(),
                    options.getChallenge().getBytes(),
                    options.getPubKeyCredParams().stream().map(p -> p.getAlg().getValue()).toList(),
                    timeout == null ? null : timeout.toMillis(),
                    exclude == null ? List.of() : exclude.stream().map(StoredDescriptor::from).toList(),
                    selection == null || selection.getAuthenticatorAttachment() == null ? null
                            : selection.getAuthenticatorAttachment().getValue(),
                    selection == null || selection.getResidentKey() == null ? null : selection.getResidentKey().getValue(),
                    selection == null || selection.getUserVerification() == null ? null
                            : selection.getUserVerification().getValue(),
                    options.getAttestation() == null ? null : options.getAttestation().getValue());
        }

        PublicKeyCredentialCreationOptions toOptions() {
            PublicKeyCredentialUserEntity user = ImmutablePublicKeyCredentialUserEntity.builder()
                    .id(new Bytes(userId))
                    .name(userName)
                    .displayName(userDisplayName)
                    .build();
            List<PublicKeyCredentialParameters> pubKeyCredParams = pubKeyCredParamAlgs.stream()
                    .map(WebAuthnCreationOptionsRepositoryAdapter::publicKeyCredentialParameters)
                    .toList();
            var builder = PublicKeyCredentialCreationOptions.builder()
                    .rp(PublicKeyCredentialRpEntity.builder().id(rpId).name(rpName).build())
                    .user(user)
                    .challenge(new Bytes(challenge))
                    .pubKeyCredParams(pubKeyCredParams)
                    .excludeCredentials(excludeCredentials.stream().map(StoredDescriptor::toDescriptor).toList());
            if (timeoutMillis != null) {
                builder.timeout(Duration.ofMillis(timeoutMillis));
            }
            if (authenticatorAttachment != null || residentKey != null || userVerification != null) {
                builder.authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                        .authenticatorAttachment(authenticatorAttachment == null ? null : lookupAuthenticatorAttachment(authenticatorAttachment))
                        .residentKey(residentKey == null ? null : residentKeyRequirement(residentKey))
                        .userVerification(userVerification == null ? null : userVerificationRequirement(userVerification))
                        .build());
            }
            if (attestation != null) {
                builder.attestation(attestationConveyancePreference(attestation));
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
                    .transports(transports.stream()
                            .map(WebAuthnCreationOptionsRepositoryAdapter::authenticatorTransport)
                            .collect(java.util.stream.Collectors.toSet()))
                    .build();
        }
    }

    // PublicKeyCredentialParameters has no values()/constructor of its own (a private constructor,
    // only these 8 named constants exist) — reconstruct by matching the stored COSE algorithm
    // value against each constant's own .getAlg().getValue(), never by constructing a new instance.
    private static final List<PublicKeyCredentialParameters> KNOWN_PARAMETERS = List.of(
            PublicKeyCredentialParameters.EdDSA, PublicKeyCredentialParameters.ES256,
            PublicKeyCredentialParameters.ES384, PublicKeyCredentialParameters.ES512,
            PublicKeyCredentialParameters.RS256, PublicKeyCredentialParameters.RS384,
            PublicKeyCredentialParameters.RS512, PublicKeyCredentialParameters.RS1);

    private static PublicKeyCredentialParameters publicKeyCredentialParameters(long algValue) {
        for (PublicKeyCredentialParameters candidate : KNOWN_PARAMETERS) {
            if (candidate.getAlg().getValue() == algValue) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unknown COSE algorithm identifier: " + algValue);
    }

    private static AuthenticatorTransport authenticatorTransport(String value) {
        for (AuthenticatorTransport candidate : AuthenticatorTransport.values()) {
            if (candidate.getValue().equals(value)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unknown authenticator transport: " + value);
    }

    private static AuthenticatorAttachment lookupAuthenticatorAttachment(String value) {
        for (AuthenticatorAttachment candidate : AuthenticatorAttachment.values()) {
            if (candidate.getValue().equals(value)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unknown authenticator attachment: " + value);
    }

    private static ResidentKeyRequirement residentKeyRequirement(String value) {
        if (ResidentKeyRequirement.DISCOURAGED.getValue().equals(value)) {
            return ResidentKeyRequirement.DISCOURAGED;
        }
        if (ResidentKeyRequirement.PREFERRED.getValue().equals(value)) {
            return ResidentKeyRequirement.PREFERRED;
        }
        if (ResidentKeyRequirement.REQUIRED.getValue().equals(value)) {
            return ResidentKeyRequirement.REQUIRED;
        }
        throw new IllegalStateException("Unknown resident key requirement: " + value);
    }

    static UserVerificationRequirement userVerificationRequirement(String value) {
        if (UserVerificationRequirement.DISCOURAGED.getValue().equals(value)) {
            return UserVerificationRequirement.DISCOURAGED;
        }
        if (UserVerificationRequirement.PREFERRED.getValue().equals(value)) {
            return UserVerificationRequirement.PREFERRED;
        }
        if (UserVerificationRequirement.REQUIRED.getValue().equals(value)) {
            return UserVerificationRequirement.REQUIRED;
        }
        throw new IllegalStateException("Unknown user verification requirement: " + value);
    }

    private static AttestationConveyancePreference attestationConveyancePreference(String value) {
        if (AttestationConveyancePreference.NONE.getValue().equals(value)) {
            return AttestationConveyancePreference.NONE;
        }
        if (AttestationConveyancePreference.INDIRECT.getValue().equals(value)) {
            return AttestationConveyancePreference.INDIRECT;
        }
        if (AttestationConveyancePreference.DIRECT.getValue().equals(value)) {
            return AttestationConveyancePreference.DIRECT;
        }
        if (AttestationConveyancePreference.ENTERPRISE.getValue().equals(value)) {
            return AttestationConveyancePreference.ENTERPRISE;
        }
        throw new IllegalStateException("Unknown attestation conveyance preference: " + value);
    }
}
