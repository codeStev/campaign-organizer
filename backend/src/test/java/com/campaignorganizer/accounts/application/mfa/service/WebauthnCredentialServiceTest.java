package com.campaignorganizer.accounts.application.mfa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.accounts.application.account.port.published.AccountQueryPort;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.application.mfa.port.out.WebAuthnCredentialRepositoryPort;
import com.campaignorganizer.accounts.application.mfa.port.out.WebAuthnCredentialSummary;
import com.campaignorganizer.accounts.domain.account.MfaMethod;
import com.campaignorganizer.accounts.domain.account.Role;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit coverage for self-service WebAuthn credential listing/removal (ADR-0111 follow-up). */
@ExtendWith(MockitoExtension.class)
class WebauthnCredentialServiceTest {

    @Mock
    private WebAuthnCredentialRepositoryPort credentials;
    @Mock
    private AccountQueryPort accounts;

    private final UUID accountId = UUID.randomUUID();
    private WebauthnCredentialService service;

    @Test
    void listsWhateverTheRepositoryReturnsUnchanged() {
        service = new WebauthnCredentialService(credentials, accounts);
        List<WebAuthnCredentialSummary> expected = List.of(summary("Laptop"), summary("Phone"));
        when(credentials.findByAccountId(accountId)).thenReturn(expected);

        assertThat(service.listWebauthnCredentials(accountId)).isEqualTo(expected);
    }

    @Test
    void removesACredentialWhenMoreThanOneExists() {
        service = new WebauthnCredentialService(credentials, accounts);
        UUID credentialId = UUID.randomUUID();
        when(accounts.findById(accountId)).thenReturn(Optional.of(accountView(MfaMethod.WEBAUTHN)));
        when(credentials.findByAccountId(accountId)).thenReturn(List.of(summary("Laptop"), summary("Phone")));
        when(credentials.deleteByIdForAccount(accountId, credentialId)).thenReturn(true);

        service.removeWebauthnCredential(accountId, credentialId);

        verify(credentials).deleteByIdForAccount(accountId, credentialId);
    }

    @Test
    void refusesToRemoveTheLastCredentialWhileWebauthnIsTheActiveMfaMethod() {
        service = new WebauthnCredentialService(credentials, accounts);
        UUID credentialId = UUID.randomUUID();
        when(accounts.findById(accountId)).thenReturn(Optional.of(accountView(MfaMethod.WEBAUTHN)));
        when(credentials.findByAccountId(accountId)).thenReturn(List.of(summary("Only one")));

        assertThatThrownBy(() -> service.removeWebauthnCredential(accountId, credentialId))
                .isInstanceOf(ValidationException.class);

        verify(credentials, never()).deleteByIdForAccount(accountId, credentialId);
    }

    @Test
    void allowsRemovingTheLastCredentialWhenWebauthnIsNotTheActiveMfaMethod() {
        // A stray leftover row on an account that switched away from WEBAUTHN via an admin
        // reset (which normally deletes every credential, but defensive coverage regardless).
        service = new WebauthnCredentialService(credentials, accounts);
        UUID credentialId = UUID.randomUUID();
        when(accounts.findById(accountId)).thenReturn(Optional.of(accountView(MfaMethod.TOTP)));
        when(credentials.deleteByIdForAccount(accountId, credentialId)).thenReturn(true);

        service.removeWebauthnCredential(accountId, credentialId);

        verify(credentials).deleteByIdForAccount(accountId, credentialId);
    }

    @Test
    void removingAnUnknownCredentialIdFails() {
        service = new WebauthnCredentialService(credentials, accounts);
        UUID credentialId = UUID.randomUUID();
        when(accounts.findById(accountId)).thenReturn(Optional.of(accountView(MfaMethod.WEBAUTHN)));
        when(credentials.findByAccountId(accountId)).thenReturn(List.of(summary("Laptop"), summary("Phone")));
        when(credentials.deleteByIdForAccount(accountId, credentialId)).thenReturn(false);

        assertThatThrownBy(() -> service.removeWebauthnCredential(accountId, credentialId))
                .isInstanceOf(NotFoundException.class);
    }

    private static WebAuthnCredentialSummary summary(String label) {
        Instant now = Instant.now();
        return new WebAuthnCredentialSummary(UUID.randomUUID(), label, now, now, Set.of());
    }

    private AccountView accountView(MfaMethod mfaMethod) {
        return new AccountView(accountId, "gm@example.com", Role.USER, true, 0, mfaMethod, Instant.now());
    }
}
