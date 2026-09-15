package com.campaignorganizer.accounts.adapter.account.out.mfa;

import com.campaignorganizer.accounts.application.account.port.out.AccountRepositoryPort;
import com.campaignorganizer.accounts.domain.account.Account;
import java.util.UUID;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.stereotype.Component;

/**
 * Implements Spring Security's own {@link PublicKeyCredentialUserEntityRepository} — the
 * "who is this ceremony for" lookup its WebAuthn filters depend on — directly against this
 * app's existing {@link AccountRepositoryPort} rather than a separate "WebAuthn user" table.
 * The account already exists by the time any WebAuthn ceremony can start (registration/login
 * both happen first), so there's nothing new to create here: {@link #save} is a no-op, and the
 * two lookups just translate an existing {@code Account} into Spring's user-entity shape,
 * keyed by {@link WebAuthnUserHandle}'s UUID-derived id.
 */
@Component
public class WebAuthnUserEntityRepositoryAdapter implements PublicKeyCredentialUserEntityRepository {

    private final AccountRepositoryPort accounts;

    public WebAuthnUserEntityRepositoryAdapter(AccountRepositoryPort accounts) {
        this.accounts = accounts;
    }

    @Override
    public PublicKeyCredentialUserEntity findById(Bytes id) {
        return accounts.findById(WebAuthnUserHandle.toAccountId(id)).map(WebAuthnUserEntityRepositoryAdapter::toUserEntity)
                .orElse(null);
    }

    /**
     * "Username" here is Spring's own lookup key, {@code Authentication.getName()} — and since
     * {@code JwtAuthFilter} sets the principal to a raw account {@link UUID} (not a
     * {@code UserDetails}/{@code Principal}), {@code getName()} falls back to
     * {@code String.valueOf(principal)}, i.e. the UUID's string form, not the email. Confirmed
     * by reading {@code Webauthn4JRelyingPartyOperations}' actual source rather than assuming —
     * getting this wrong would silently 400 every WebAuthn ceremony (this repository never
     * finding a match) despite compiling and looking plausible.
     */
    @Override
    public PublicKeyCredentialUserEntity findByUsername(String username) {
        UUID accountId;
        try {
            accountId = UUID.fromString(username);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        return accounts.findById(accountId).map(WebAuthnUserEntityRepositoryAdapter::toUserEntity).orElse(null);
    }

    @Override
    public void save(PublicKeyCredentialUserEntity userEntity) {
        // No-op: the account this represents already exists (see class Javadoc).
    }

    @Override
    public void delete(Bytes id) {
        // No-op: deleting the WebAuthn "user" would mean deleting the account itself, which
        // this repository has no business doing — account deletion goes through the accounts
        // context's own DeleteAccountUseCase.
    }

    private static PublicKeyCredentialUserEntity toUserEntity(Account account) {
        return ImmutablePublicKeyCredentialUserEntity.builder()
                .id(WebAuthnUserHandle.toBytes(account.getId()))
                .name(account.getEmail())
                .displayName(account.getEmail())
                .build();
    }
}
