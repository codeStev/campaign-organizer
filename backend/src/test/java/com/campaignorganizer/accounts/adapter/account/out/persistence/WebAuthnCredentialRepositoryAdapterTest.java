package com.campaignorganizer.accounts.adapter.account.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.accounts.adapter.account.out.mfa.WebAuthnUserHandle;
import com.campaignorganizer.accounts.application.account.port.published.AccountQueryPort;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.domain.account.MfaMethod;
import com.campaignorganizer.accounts.domain.account.Role;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;

/**
 * Unit coverage for the enrollment-race guard added during this feature's own security-review
 * pass (see the class Javadoc on {@link WebAuthnCredentialRepositoryAdapter}): a brand-new
 * credential can only be planted for an account with no active MFA method yet, but an *update*
 * to an existing credential (the normal signature-count bump on every login) is unaffected.
 */
@ExtendWith(MockitoExtension.class)
class WebAuthnCredentialRepositoryAdapterTest {

    @Mock
    private WebAuthnCredentialJpaRepository repository;
    @Mock
    private AccountQueryPort accounts;

    private final UUID accountId = UUID.randomUUID();
    private final byte[] credentialId = "a-credential-id".getBytes();

    @Test
    void savesANewCredentialForAnAccountWithNoActiveMfaMethod() {
        WebAuthnCredentialRepositoryAdapter adapter = new WebAuthnCredentialRepositoryAdapter(repository, accounts);
        when(repository.findByCredentialId(credentialId)).thenReturn(Optional.empty());
        when(accounts.findById(accountId)).thenReturn(Optional.of(accountView(MfaMethod.NONE)));

        adapter.save(credentialRecord());

        verify(repository).save(any());
    }

    @Test
    void refusesANewCredentialForAnAccountThatAlreadyHasAnActiveMfaMethod() {
        WebAuthnCredentialRepositoryAdapter adapter = new WebAuthnCredentialRepositoryAdapter(repository, accounts);
        when(repository.findByCredentialId(credentialId)).thenReturn(Optional.empty());
        when(accounts.findById(accountId)).thenReturn(Optional.of(accountView(MfaMethod.TOTP)));

        assertThatThrownBy(() -> adapter.save(credentialRecord())).isInstanceOf(AccessDeniedException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void updatingAnExistingCredentialSkipsTheMfaMethodCheck() {
        WebAuthnCredentialRepositoryAdapter adapter = new WebAuthnCredentialRepositoryAdapter(repository, accounts);
        WebAuthnCredentialJpaEntity existing = new WebAuthnCredentialJpaEntity();
        existing.setId(UUID.randomUUID());
        existing.setAccountId(accountId);
        when(repository.findByCredentialId(credentialId)).thenReturn(Optional.of(existing));

        // A normal signature-count bump after a successful login, on an account whose active
        // method is already WEBAUTHN — accounts.findById is never even consulted here.
        adapter.save(credentialRecord());

        verify(repository).save(existing);
        assertThat(existing.getAccountId()).isEqualTo(accountId);
    }

    private CredentialRecord credentialRecord() {
        Instant now = Instant.now();
        return ImmutableCredentialRecord.builder()
                .credentialType(PublicKeyCredentialType.PUBLIC_KEY)
                .credentialId(new Bytes(credentialId))
                .userEntityUserId(WebAuthnUserHandle.toBytes(accountId))
                .publicKey(new ImmutablePublicKeyCose(new byte[] {1, 2, 3}))
                .signatureCount(0)
                .uvInitialized(true)
                .transports(Set.of())
                .backupEligible(false)
                .backupState(false)
                .attestationObject(new Bytes(new byte[] {4, 5, 6}))
                .attestationClientDataJSON(new Bytes(new byte[] {7, 8, 9}))
                .label("Passkey")
                .created(now)
                .lastUsed(now)
                .build();
    }

    private AccountView accountView(MfaMethod mfaMethod) {
        return new AccountView(accountId, "gm@example.com", Role.USER, true, 0, mfaMethod, Instant.now());
    }
}
