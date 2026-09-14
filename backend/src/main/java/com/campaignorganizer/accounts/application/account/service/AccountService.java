package com.campaignorganizer.accounts.application.account.service;

import com.campaignorganizer.accounts.application.account.port.in.AccountCommands.RegisterAccountCommand;
import com.campaignorganizer.accounts.application.account.port.in.ChangeOwnPasswordUseCase;
import com.campaignorganizer.accounts.application.account.port.in.DeleteAccountUseCase;
import com.campaignorganizer.accounts.application.account.port.in.GetAccountUseCase;
import com.campaignorganizer.accounts.application.account.port.in.ListAccountsUseCase;
import com.campaignorganizer.accounts.application.account.port.in.LogoutAllUseCase;
import com.campaignorganizer.accounts.application.account.port.in.RegisterAccountUseCase;
import com.campaignorganizer.accounts.application.account.port.in.ResetPasswordUseCase;
import com.campaignorganizer.accounts.application.account.port.in.SetEnabledUseCase;
import com.campaignorganizer.accounts.application.account.port.in.UpdateRoleUseCase;
import com.campaignorganizer.accounts.application.account.port.out.AccountRepositoryPort;
import com.campaignorganizer.accounts.application.account.port.published.AccountQueryPort;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.application.account.port.published.AuthenticateAccountPort;
import com.campaignorganizer.accounts.domain.account.Account;
import com.campaignorganizer.accounts.domain.account.Role;
import com.campaignorganizer.ai.application.port.published.AiSettingsOwnershipPort;
import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockOwnershipPort;
import com.campaignorganizer.characters.application.template.port.published.GameSystemOwnershipPort;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateOwnershipPort;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldOwnershipPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Account use cases; also implements the published authenticate/query ports for every other bounded context. */
@Service
public class AccountService implements RegisterAccountUseCase, ListAccountsUseCase, GetAccountUseCase,
        UpdateRoleUseCase, SetEnabledUseCase, ResetPasswordUseCase, ChangeOwnPasswordUseCase, LogoutAllUseCase,
        DeleteAccountUseCase, AuthenticateAccountPort, AccountQueryPort {

    /**
     * A precomputed BCrypt hash of an arbitrary string nobody will ever submit as a
     * password. Used so a login attempt against a nonexistent email still pays the
     * same hashing cost as one against a real account (ADR-0110, anti-enumeration) —
     * without this, "no such account" would return measurably faster than "wrong
     * password" and the timing itself would leak which emails are registered.
     */
    private static final String DUMMY_BCRYPT_HASH =
            "$2b$10$cWqcGdYHljaqmQLUzWYbuepe0s8SVS.hPSJl8E8cEooByWllxQu7m";

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(5);

    private final AccountRepositoryPort accounts;
    private final PasswordEncoder passwordEncoder;
    private final IdGenerator ids;
    private final Clock clock;
    private final WorldOwnershipPort worldOwnership;
    private final AiSettingsOwnershipPort aiSettingsOwnership;
    private final GameSystemOwnershipPort gameSystemOwnership;
    private final GlobalFieldTemplateOwnershipPort globalFieldTemplateOwnership;
    private final GlobalStatblockOwnershipPort globalStatblockOwnership;

    public AccountService(AccountRepositoryPort accounts, PasswordEncoder passwordEncoder, IdGenerator ids,
                          Clock clock, WorldOwnershipPort worldOwnership,
                          AiSettingsOwnershipPort aiSettingsOwnership, GameSystemOwnershipPort gameSystemOwnership,
                          GlobalFieldTemplateOwnershipPort globalFieldTemplateOwnership,
                          GlobalStatblockOwnershipPort globalStatblockOwnership) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.ids = ids;
        this.clock = clock;
        this.worldOwnership = worldOwnership;
        this.aiSettingsOwnership = aiSettingsOwnership;
        this.gameSystemOwnership = gameSystemOwnership;
        this.globalFieldTemplateOwnership = globalFieldTemplateOwnership;
        this.globalStatblockOwnership = globalStatblockOwnership;
    }

    @Override
    @Transactional
    public void register(RegisterAccountCommand command) {
        // Anti-enumeration (ADR-0110): both branches below take comparable time and
        // the caller learns nothing about which one ran.
        if (accounts.existsByEmailIgnoreCase(command.email())) {
            passwordEncoder.encode(command.rawPassword());
            return;
        }
        boolean firstAccount = accounts.count() == 0;
        Role role = firstAccount ? Role.ADMIN : Role.USER;
        String hash = passwordEncoder.encode(command.rawPassword());
        Account account = Account.create(ids.newId(), command.email(), hash, role, clock.instant());
        accounts.save(account);
        if (firstAccount) {
            worldOwnership.assignUnownedTo(account.getId());
            aiSettingsOwnership.assignUnownedTo(account.getId());
            gameSystemOwnership.assignUnownedTo(account.getId());
            globalFieldTemplateOwnership.assignUnownedTo(account.getId());
            globalStatblockOwnership.assignUnownedTo(account.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountView> list() {
        return accounts.findAllOrderByCreatedAtDesc().stream().map(AccountService::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountView get(UUID accountId) {
        return toView(require(accountId));
    }

    @Override
    @Transactional
    public AccountView updateRole(UUID accountId, Role role) {
        Account account = require(accountId);
        account.changeRole(role, clock.instant());
        return toView(accounts.save(account));
    }

    @Override
    @Transactional
    public AccountView setEnabled(UUID accountId, boolean enabled) {
        Account account = require(accountId);
        if (enabled) {
            account.enable(clock.instant());
        } else {
            account.disable(clock.instant());
        }
        return toView(accounts.save(account));
    }

    @Override
    @Transactional
    public void resetPassword(UUID accountId, String newPassword) {
        Account account = require(accountId);
        account.changePasswordHash(passwordEncoder.encode(newPassword), clock.instant());
        accounts.save(account);
    }

    @Override
    @Transactional
    public void changeOwnPassword(UUID accountId, String currentPassword, String newPassword) {
        Account account = require(accountId);
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect");
        }
        account.changePasswordHash(passwordEncoder.encode(newPassword), clock.instant());
        accounts.save(account);
    }

    @Override
    @Transactional
    public void logoutAll(UUID accountId) {
        Account account = require(accountId);
        account.logoutAll(clock.instant());
        accounts.save(account);
    }

    @Override
    @Transactional
    public void delete(UUID accountId) {
        accounts.delete(require(accountId));
    }

    // --- published ports ---

    @Override
    @Transactional
    public Optional<AccountView> authenticate(String email, String rawPassword) {
        Instant now = clock.instant();
        Optional<Account> found = accounts.findByEmailIgnoreCase(email);
        if (found.isEmpty()) {
            passwordEncoder.matches(rawPassword, DUMMY_BCRYPT_HASH);
            return Optional.empty();
        }
        Account account = found.get();
        // Always run the real comparison, even if the account is locked/disabled —
        // branching on those first would let their timing distinguish this case
        // from a plain wrong-password attempt.
        boolean passwordMatches = passwordEncoder.matches(rawPassword, account.getPasswordHash());
        if (account.isLockedAt(now) || !account.isEnabled()) {
            return Optional.empty();
        }
        if (!passwordMatches) {
            account.recordFailedLogin(now, MAX_FAILED_ATTEMPTS, LOCKOUT_DURATION);
            accounts.save(account);
            return Optional.empty();
        }
        account.recordSuccessfulLogin(now);
        return Optional.of(toView(accounts.save(account)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountView> findById(UUID accountId) {
        return accounts.findById(accountId).map(AccountService::toView);
    }

    private Account require(UUID accountId) {
        return accounts.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }

    private static AccountView toView(Account account) {
        return new AccountView(account.getId(), account.getEmail(), account.getRole(), account.isEnabled(),
                account.getTokenVersion(), account.getMfaMethod(), account.getCreatedAt());
    }
}
