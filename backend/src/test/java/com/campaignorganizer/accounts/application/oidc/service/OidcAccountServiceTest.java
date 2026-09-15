package com.campaignorganizer.accounts.application.oidc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.accounts.application.account.port.out.AccountRepositoryPort;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.application.account.service.FirstAccountOwnershipBootstrapper;
import com.campaignorganizer.accounts.domain.account.Account;
import com.campaignorganizer.accounts.domain.account.Role;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.ConflictException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit coverage for Google sign-in account resolution (ADR-0113). */
@ExtendWith(MockitoExtension.class)
class OidcAccountServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-03T00:00:00Z");

    @Mock
    private AccountRepositoryPort accounts;
    @Mock
    private IdGenerator ids;
    @Mock
    private FirstAccountOwnershipBootstrapper ownershipBootstrapper;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private OidcAccountService service;

    @BeforeEach
    void setUp() {
        service = new OidcAccountService(accounts, ids, clock, ownershipBootstrapper);
    }

    @Test
    void repeatLoginReturnsTheExistingAccountUnchanged() {
        UUID accountId = UUID.randomUUID();
        Account existing = Account.createViaOidc(accountId, "gm@example.com", "GOOGLE", "subject-1", Role.USER, NOW);
        when(accounts.findByProviderAndSubject("GOOGLE", "subject-1")).thenReturn(Optional.of(existing));

        AccountView result = service.authenticateOrRegister("GOOGLE", "subject-1", "gm@example.com");

        assertThat(result.id()).isEqualTo(accountId);
        verify(accounts, never()).save(any());
        verify(ownershipBootstrapper, never()).assignInitialOwnership(any());
    }

    @Test
    void newIdentityBecomesAdminWhenItIsTheVeryFirstAccount() {
        UUID newId = UUID.randomUUID();
        when(accounts.findByProviderAndSubject("GOOGLE", "subject-1")).thenReturn(Optional.empty());
        when(accounts.existsByEmailIgnoreCase("gm@example.com")).thenReturn(false);
        when(accounts.count()).thenReturn(0L);
        when(ids.newId()).thenReturn(newId);
        when(accounts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AccountView result = service.authenticateOrRegister("GOOGLE", "subject-1", "gm@example.com");

        assertThat(result.role()).isEqualTo(Role.ADMIN);
        verify(ownershipBootstrapper).assignInitialOwnership(newId);
    }

    @Test
    void newIdentityBecomesUserWhenAnAccountAlreadyExists() {
        UUID newId = UUID.randomUUID();
        when(accounts.findByProviderAndSubject("GOOGLE", "subject-1")).thenReturn(Optional.empty());
        when(accounts.existsByEmailIgnoreCase("gm@example.com")).thenReturn(false);
        when(accounts.count()).thenReturn(1L);
        when(ids.newId()).thenReturn(newId);
        when(accounts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AccountView result = service.authenticateOrRegister("GOOGLE", "subject-1", "gm@example.com");

        assertThat(result.role()).isEqualTo(Role.USER);
        verify(ownershipBootstrapper, never()).assignInitialOwnership(any());
    }

    @Test
    void refusesAGoogleLoginWhoseEmailAlreadyBelongsToADifferentAccount() {
        when(accounts.findByProviderAndSubject("GOOGLE", "subject-1")).thenReturn(Optional.empty());
        when(accounts.existsByEmailIgnoreCase("gm@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.authenticateOrRegister("GOOGLE", "subject-1", "gm@example.com"))
                .isInstanceOf(ConflictException.class);

        verify(accounts, never()).save(any());
    }
}
