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
import com.campaignorganizer.security.JwtService;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;

/**
 * Unit coverage for the enrollment-race guard added during PR #87's own security-review pass,
 * and loosened (not removed) when self-service credential management was added (see the class
 * Javadoc on {@link WebAuthnCredentialRepositoryAdapter}): a brand-new credential can be
 * planted for an account with no active MFA method yet (first enrollment), or for an
 * already-WEBAUTHN account only when the current request already carries the MFA factor
 * (adding a backup credential) — never for a PASSWORD-only caller against an already-enrolled
 * account. An *update* to an existing credential (the normal signature-count bump on every
 * login) is unaffected either way.
 */
@ExtendWith(MockitoExtension.class)
class WebAuthnCredentialRepositoryAdapterTest {

    @Mock
    private WebAuthnCredentialJpaRepository repository;
    @Mock
    private AccountQueryPort accounts;

    private final UUID accountId = UUID.randomUUID();
    private final byte[] credentialId = "a-credential-id".getBytes();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

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
    void savesAnAdditionalCredentialWhenTheCurrentRequestAlreadyHasTheMfaFactor() {
        WebAuthnCredentialRepositoryAdapter adapter = new WebAuthnCredentialRepositoryAdapter(repository, accounts);
        when(repository.findByCredentialId(credentialId)).thenReturn(Optional.empty());
        when(accounts.findById(accountId)).thenReturn(Optional.of(accountView(MfaMethod.WEBAUTHN)));
        authenticateWithAuthorities(JwtService.MFA_AUTHORITY);

        adapter.save(credentialRecord());

        verify(repository).save(any());
    }

    @Test
    void refusesAnAdditionalCredentialWhenTheCurrentRequestOnlyHasThePasswordFactor() {
        WebAuthnCredentialRepositoryAdapter adapter = new WebAuthnCredentialRepositoryAdapter(repository, accounts);
        when(repository.findByCredentialId(credentialId)).thenReturn(Optional.empty());
        when(accounts.findById(accountId)).thenReturn(Optional.of(accountView(MfaMethod.WEBAUTHN)));
        authenticateWithAuthorities("FACTOR_PASSWORD");

        assertThatThrownBy(() -> adapter.save(credentialRecord())).isInstanceOf(AccessDeniedException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void refusesAnAdditionalCredentialWhenThereIsNoAuthenticationAtAll() {
        WebAuthnCredentialRepositoryAdapter adapter = new WebAuthnCredentialRepositoryAdapter(repository, accounts);
        when(repository.findByCredentialId(credentialId)).thenReturn(Optional.empty());
        when(accounts.findById(accountId)).thenReturn(Optional.of(accountView(MfaMethod.WEBAUTHN)));

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

    @Test
    void findByAccountIdMapsToSummariesExcludingSensitiveFields() {
        WebAuthnCredentialRepositoryAdapter adapter = new WebAuthnCredentialRepositoryAdapter(repository, accounts);
        WebAuthnCredentialJpaEntity entity = new WebAuthnCredentialJpaEntity();
        UUID rowId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant lastUsedAt = Instant.parse("2026-02-01T00:00:00Z");
        entity.setId(rowId);
        entity.setAccountId(accountId);
        entity.setLabel("YubiKey");
        entity.setTransports("usb,nfc");
        entity.setCreatedAt(createdAt);
        entity.setLastUsedAt(lastUsedAt);
        when(repository.findByAccountIdOrderByCreatedAtAsc(accountId)).thenReturn(List.of(entity));

        var summaries = adapter.findByAccountId(accountId);

        assertThat(summaries).hasSize(1);
        var summary = summaries.get(0);
        assertThat(summary.id()).isEqualTo(rowId);
        assertThat(summary.label()).isEqualTo("YubiKey");
        assertThat(summary.createdAt()).isEqualTo(createdAt);
        assertThat(summary.lastUsedAt()).isEqualTo(lastUsedAt);
        assertThat(summary.transports()).containsExactlyInAnyOrder("usb", "nfc");
    }

    @Test
    void deleteByIdForAccountDelegatesScopedByBothIdAndAccount() {
        WebAuthnCredentialRepositoryAdapter adapter = new WebAuthnCredentialRepositoryAdapter(repository, accounts);
        UUID rowId = UUID.randomUUID();
        when(repository.deleteByIdAndAccountId(rowId, accountId)).thenReturn(1);

        boolean deleted = adapter.deleteByIdForAccount(accountId, rowId);

        assertThat(deleted).isTrue();
    }

    @Test
    void deleteByIdForAccountReturnsFalseWhenNothingMatched() {
        WebAuthnCredentialRepositoryAdapter adapter = new WebAuthnCredentialRepositoryAdapter(repository, accounts);
        UUID rowId = UUID.randomUUID();
        when(repository.deleteByIdAndAccountId(rowId, accountId)).thenReturn(0);

        boolean deleted = adapter.deleteByIdForAccount(accountId, rowId);

        assertThat(deleted).isFalse();
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

    private void authenticateWithAuthorities(String... authorities) {
        List<SimpleGrantedAuthority> granted = Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(accountId, null, granted));
    }
}
