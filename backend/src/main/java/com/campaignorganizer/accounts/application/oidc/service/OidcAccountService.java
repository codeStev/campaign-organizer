package com.campaignorganizer.accounts.application.oidc.service;

import com.campaignorganizer.accounts.application.account.port.out.AccountRepositoryPort;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.application.account.service.FirstAccountOwnershipBootstrapper;
import com.campaignorganizer.accounts.application.oidc.port.in.AuthenticateOrRegisterViaOidcUseCase;
import com.campaignorganizer.accounts.domain.account.Account;
import com.campaignorganizer.accounts.domain.account.Role;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.ConflictException;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account resolution for Google sign-in (ADR-0113) — kept separate from {@code AccountService},
 * the same way {@code MfaService} is already split out, since this is a distinct identity
 * concern (external-provider login) rather than password-account CRUD.
 */
@Service
public class OidcAccountService implements AuthenticateOrRegisterViaOidcUseCase {

    private final AccountRepositoryPort accounts;
    private final IdGenerator ids;
    private final Clock clock;
    private final FirstAccountOwnershipBootstrapper ownershipBootstrapper;

    public OidcAccountService(AccountRepositoryPort accounts, IdGenerator ids, Clock clock,
                              FirstAccountOwnershipBootstrapper ownershipBootstrapper) {
        this.accounts = accounts;
        this.ids = ids;
        this.clock = clock;
        this.ownershipBootstrapper = ownershipBootstrapper;
    }

    @Override
    @Transactional
    public AccountView authenticateOrRegister(String authProvider, String externalSubject, String email) {
        Optional<Account> existing = accounts.findByProviderAndSubject(authProvider, externalSubject);
        if (existing.isPresent()) {
            return toView(existing.get());
        }
        if (accounts.existsByEmailIgnoreCase(email)) {
            throw new ConflictException(
                    "An account already exists for this email. Sign in with your password instead.");
        }
        boolean firstAccount = accounts.count() == 0;
        Role role = firstAccount ? Role.ADMIN : Role.USER;
        Account account = Account.createViaOidc(ids.newId(), email, authProvider, externalSubject, role,
                clock.instant());
        accounts.save(account);
        if (firstAccount) {
            ownershipBootstrapper.assignInitialOwnership(account.getId());
        }
        return toView(account);
    }

    private static AccountView toView(Account account) {
        return new AccountView(account.getId(), account.getEmail(), account.getRole(), account.isEnabled(),
                account.getTokenVersion(), account.getMfaMethod(), account.getCreatedAt());
    }
}
