package com.campaignorganizer.accounts.adapter.account.out.persistence;

import com.campaignorganizer.accounts.adapter.account.out.mfa.WebAuthnUserHandle;
import com.campaignorganizer.accounts.application.account.port.published.AccountQueryPort;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.application.mfa.port.out.WebAuthnCredentialRepositoryPort;
import com.campaignorganizer.accounts.application.mfa.port.out.WebAuthnCredentialSummary;
import com.campaignorganizer.accounts.domain.account.MfaMethod;
import com.campaignorganizer.security.JwtService;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
 * MfaService}/{@code WebauthnCredentialService} need (ADR-0111 follow-up: admin-triggered MFA
 * reset must delete every credential, not just clear the account's mfaMethod flag; self-service
 * credential management needs to list/remove one).
 *
 * <p>{@link #save} refuses to persist a *new* credential unless the account has no active MFA
 * method yet (first-time enrollment) or the *current request* already carries the MFA factor
 * (adding a backup credential to an already-WEBAUTHN account) — found missing during PR #87's
 * own {@code security-review} pass, and re-checked here rather than loosened away when self-
 * service credential management was added. {@code WebAuthnRegistrationFilter} (Spring's own,
 * the only caller of this method) has no authorization check beyond "holds some valid
 * PASSWORD-factor token", so without this guard an attacker who only has an account's leaked
 * password (not its real, already-active second factor) could silently plant their own passkey
 * here and use it to log in indefinitely — completely bypassing whatever second factor the
 * legitimate owner actually has configured. {@code Account.beginTotpEnrollment} enforces the
 * same invariant on the TOTP side (chosen once at enrollment); this mirrors it for WebAuthn's
 * *first* credential, while still allowing an already-MFA'd owner to register a spare one.
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
            boolean firstEnrollment = account.mfaMethod() == MfaMethod.NONE;
            boolean addingBackupCredential = account.mfaMethod() == MfaMethod.WEBAUTHN && currentRequestHasMfaFactor();
            if (!firstEnrollment && !addingBackupCredential) {
                throw new AccessDeniedException("Not authorized to register a new WebAuthn credential for this account");
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

    @Override
    public List<WebAuthnCredentialSummary> findByAccountId(UUID accountId) {
        return repository.findByAccountIdOrderByCreatedAtAsc(accountId).stream()
                .map(WebAuthnCredentialRepositoryAdapter::toSummary)
                .toList();
    }

    @Override
    public boolean deleteByIdForAccount(UUID accountId, UUID id) {
        return repository.deleteByIdAndAccountId(id, accountId) > 0;
    }

    /** Same technique as {@code WebAuthnRequestOptionsRepositoryAdapter.requireAccountId()}. */
    private static boolean currentRequestHasMfaFactor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> JwtService.MFA_AUTHORITY.equals(authority.getAuthority()));
    }

    private static WebAuthnCredentialSummary toSummary(WebAuthnCredentialJpaEntity entity) {
        String stored = entity.getTransports();
        Set<String> transports = (stored == null || stored.isBlank())
                ? Set.of()
                : Arrays.stream(stored.split(",")).collect(Collectors.toSet());
        return new WebAuthnCredentialSummary(entity.getId(), entity.getLabel(), entity.getCreatedAt(),
                entity.getLastUsedAt(), transports);
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
