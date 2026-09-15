package com.campaignorganizer.accounts.adapter.account.out.persistence;

import com.campaignorganizer.accounts.adapter.account.out.mfa.WebAuthnUserHandle;
import com.campaignorganizer.accounts.application.account.port.published.AccountQueryPort;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.application.mfa.port.out.WebAuthnCredentialRepositoryPort;
import com.campaignorganizer.accounts.domain.account.MfaMethod;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.stereotype.Component;

/**
 * Implements Spring Security's own {@link UserCredentialRepository} directly — the credential
 * store its WebAuthn ceremony filters depend on, autodetected as a bean by the {@code
 * .webAuthn()} DSL — backed by this project's own JPA table rather than Spring's hand-SQL
 * {@code JdbcUserCredentialRepository}, for consistency with every other persistence adapter
 * here. Also implements {@link WebAuthnCredentialRepositoryPort}, the narrow slice {@code
 * MfaService} itself needs (ADR-0111 follow-up: admin-triggered MFA reset must delete the
 * credential too, not just clear the account's mfaMethod flag).
 *
 * <p>{@link #save} refuses to persist a *new* credential for an account whose
 * {@code mfaMethod} isn't already {@code NONE} — found missing during this feature's own
 * {@code security-review} pass. {@code WebAuthnRegistrationFilter} (Spring's own, the only
 * caller of this method) has no authorization check beyond "holds some valid PASSWORD-factor
 * token", so without this guard an attacker who only has an account's leaked password (not its
 * real, already-active second factor) could silently plant their own passkey here and use it to
 * log in indefinitely — completely bypassing whatever second factor the legitimate owner
 * actually has configured. {@code Account.beginTotpEnrollment} enforces the same invariant on
 * the TOTP side; this is its WebAuthn-side equivalent, just enforced here rather than on the
 * domain aggregate, since the credential itself never touches {@code Account}.
 */
@Component
public class WebAuthnCredentialRepositoryAdapter implements UserCredentialRepository, WebAuthnCredentialRepositoryPort {

    private final WebAuthnCredentialJpaRepository repository;
    private final AccountQueryPort accounts;

    public WebAuthnCredentialRepositoryAdapter(WebAuthnCredentialJpaRepository repository, AccountQueryPort accounts) {
        this.repository = repository;
        this.accounts = accounts;
    }

    @Override
    public void save(CredentialRecord credentialRecord) {
        WebAuthnCredentialJpaEntity entity = repository.findByCredentialId(credentialRecord.getCredentialId().getBytes())
                .orElseGet(WebAuthnCredentialJpaEntity::new);
        if (entity.getId() == null) {
            UUID accountId = WebAuthnUserHandle.toAccountId(credentialRecord.getUserEntityUserId());
            AccountView account = accounts.findById(accountId)
                    .orElseThrow(() -> new AccessDeniedException("No such account"));
            if (account.mfaMethod() != MfaMethod.NONE) {
                throw new AccessDeniedException("Account already has an active MFA method");
            }
            entity.setId(UUID.randomUUID());
            entity.setAccountId(accountId);
            entity.setCreatedAt(credentialRecord.getCreated());
        }
        entity.setCredentialId(credentialRecord.getCredentialId().getBytes());
        entity.setCredentialType(credentialRecord.getCredentialType().getValue());
        entity.setPublicKey(credentialRecord.getPublicKey().getBytes());
        entity.setSignatureCount(credentialRecord.getSignatureCount());
        entity.setUvInitialized(credentialRecord.isUvInitialized());
        entity.setTransports(serializeTransports(credentialRecord.getTransports()));
        entity.setBackupEligible(credentialRecord.isBackupEligible());
        entity.setBackupState(credentialRecord.isBackupState());
        entity.setAttestationObject(credentialRecord.getAttestationObject().getBytes());
        entity.setAttestationClientDataJson(credentialRecord.getAttestationClientDataJSON().getBytes());
        entity.setLabel(credentialRecord.getLabel());
        entity.setLastUsedAt(credentialRecord.getLastUsed());
        repository.save(entity);
    }

    @Override
    public CredentialRecord findByCredentialId(Bytes credentialId) {
        return repository.findByCredentialId(credentialId.getBytes()).map(this::toCredentialRecord).orElse(null);
    }

    @Override
    public List<CredentialRecord> findByUserId(Bytes userId) {
        return repository.findByAccountId(WebAuthnUserHandle.toAccountId(userId)).stream()
                .map(this::toCredentialRecord)
                .toList();
    }

    @Override
    public void delete(Bytes credentialId) {
        repository.deleteByCredentialId(credentialId.getBytes());
    }

    @Override
    public boolean existsByAccountId(UUID accountId) {
        return repository.existsByAccountId(accountId);
    }

    @Override
    public void deleteByAccountId(UUID accountId) {
        repository.deleteByAccountId(accountId);
    }

    private CredentialRecord toCredentialRecord(WebAuthnCredentialJpaEntity entity) {
        return ImmutableCredentialRecord.builder()
                .credentialType(PublicKeyCredentialType.valueOf(entity.getCredentialType()))
                .credentialId(new Bytes(entity.getCredentialId()))
                .userEntityUserId(WebAuthnUserHandle.toBytes(entity.getAccountId()))
                .publicKey(new ImmutablePublicKeyCose(entity.getPublicKey()))
                .signatureCount(entity.getSignatureCount())
                .uvInitialized(entity.isUvInitialized())
                .transports(parseTransports(entity.getTransports()))
                .backupEligible(entity.isBackupEligible())
                .backupState(entity.isBackupState())
                .attestationObject(new Bytes(entity.getAttestationObject()))
                .attestationClientDataJSON(new Bytes(entity.getAttestationClientDataJson()))
                .label(entity.getLabel())
                .created(entity.getCreatedAt())
                .lastUsed(entity.getLastUsedAt())
                .build();
    }

    private static String serializeTransports(Set<AuthenticatorTransport> transports) {
        return transports.stream().map(AuthenticatorTransport::getValue).collect(Collectors.joining(","));
    }

    private static Set<AuthenticatorTransport> parseTransports(String stored) {
        if (stored == null || stored.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(stored.split(",")).map(AuthenticatorTransport::valueOf).collect(Collectors.toSet());
    }
}
