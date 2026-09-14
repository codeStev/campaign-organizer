package com.campaignorganizer.accounts.application.mfa.service;

import com.campaignorganizer.accounts.application.account.port.out.AccountRepositoryPort;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.application.mfa.port.in.ConfirmTotpSetupUseCase;
import com.campaignorganizer.accounts.application.mfa.port.in.MfaResults.MfaEnrollmentOutcome;
import com.campaignorganizer.accounts.application.mfa.port.in.MfaResults.TotpSetupStart;
import com.campaignorganizer.accounts.application.mfa.port.in.RecoverPasswordUseCase;
import com.campaignorganizer.accounts.application.mfa.port.in.ResetMfaUseCase;
import com.campaignorganizer.accounts.application.mfa.port.in.StartTotpSetupUseCase;
import com.campaignorganizer.accounts.application.mfa.port.in.VerifyRecoveryCodeUseCase;
import com.campaignorganizer.accounts.application.mfa.port.in.VerifyTotpChallengeUseCase;
import com.campaignorganizer.accounts.application.mfa.port.out.RecoveryCodePort;
import com.campaignorganizer.accounts.application.mfa.port.out.RecoveryCodeRepositoryPort;
import com.campaignorganizer.accounts.application.account.port.out.TotpPort;
import com.campaignorganizer.accounts.domain.account.Account;
import com.campaignorganizer.accounts.domain.account.MfaMethod;
import com.campaignorganizer.accounts.domain.recoverycode.RecoveryCode;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.AuthenticationFailedException;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TOTP enrollment/challenge and recovery-code use cases (ADR-0111) — kept out of
 * {@code AccountService} to keep that class from growing further, same reasoning as the
 * query-service split elsewhere in this codebase. WebAuthn enrollment/challenge is handled
 * separately, by Spring Security's own {@code .webAuthn()} support, not this service.
 */
@Service
public class MfaService implements StartTotpSetupUseCase, ConfirmTotpSetupUseCase, VerifyTotpChallengeUseCase,
        VerifyRecoveryCodeUseCase, RecoverPasswordUseCase, ResetMfaUseCase {

    private static final int RECOVERY_CODE_COUNT = 10;

    /**
     * Same anti-enumeration idea as {@code AccountService.authenticate}'s DUMMY_BCRYPT_HASH:
     * an arbitrary but fixed valid-format BCrypt hash nobody's real recovery code will ever
     * match, so an unknown email pays a comparable cost to a known one before returning.
     */
    private static final String DUMMY_BCRYPT_HASH =
            "$2b$10$cWqcGdYHljaqmQLUzWYbuepe0s8SVS.hPSJl8E8cEooByWllxQu7m";

    private final AccountRepositoryPort accounts;
    private final RecoveryCodeRepositoryPort recoveryCodeRepository;
    private final TotpPort totp;
    private final RecoveryCodePort recoveryCodeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final TextEncryptor totpSecretEncryptor;
    private final IdGenerator ids;
    private final Clock clock;

    public MfaService(AccountRepositoryPort accounts, RecoveryCodeRepositoryPort recoveryCodeRepository,
                      TotpPort totp, RecoveryCodePort recoveryCodeGenerator, PasswordEncoder passwordEncoder,
                      TextEncryptor totpSecretEncryptor, IdGenerator ids, Clock clock) {
        this.accounts = accounts;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.totp = totp;
        this.recoveryCodeGenerator = recoveryCodeGenerator;
        this.passwordEncoder = passwordEncoder;
        this.totpSecretEncryptor = totpSecretEncryptor;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TotpSetupStart startTotpSetup(UUID accountId) {
        Account account = require(accountId);
        String secret = totp.generateSecret();
        // Account.beginTotpEnrollment rejects this if a method is already active — no need
        // to duplicate that check here.
        account.beginTotpEnrollment(totpSecretEncryptor.encrypt(secret), clock.instant());
        accounts.save(account);
        return new TotpSetupStart(secret, totp.provisioningUri(secret, account.getEmail()),
                totp.qrCodeDataUri(secret, account.getEmail()));
    }

    @Override
    @Transactional
    public MfaEnrollmentOutcome confirmTotpSetup(UUID accountId, String code) {
        Account account = require(accountId);
        String pendingEncrypted = account.getTotpSecretPendingEncrypted();
        if (pendingEncrypted == null) {
            throw new ValidationException("No pending TOTP enrollment to confirm — call setup/totp/start first");
        }
        if (!totp.verifyCode(totpSecretEncryptor.decrypt(pendingEncrypted), code)) {
            throw new AuthenticationFailedException("Invalid TOTP code");
        }
        account.completeTotpEnrollment(clock.instant());
        accounts.save(account);
        return new MfaEnrollmentOutcome(toView(account), issueRecoveryCodes(account.getId()));
    }

    @Override
    @Transactional
    public AccountView verifyTotpChallenge(UUID accountId, String code) {
        Account account = require(accountId);
        if (account.getMfaMethod() != MfaMethod.TOTP || !totp.verifyCode(
                totpSecretEncryptor.decrypt(account.getTotpSecretEncrypted()), code)) {
            throw new AuthenticationFailedException("Invalid TOTP code");
        }
        return toView(account);
    }

    @Override
    @Transactional
    public AccountView verifyRecoveryCode(UUID accountId, String recoveryCode) {
        Account account = require(accountId);
        RecoveryCode matched = findMatchingUnusedCode(account.getId(), recoveryCode)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid recovery code"));
        matched.consume(clock.instant());
        recoveryCodeRepository.save(matched);
        // The lost device may be gone for good — force re-enrollment through the normal setup
        // path rather than granting access with the old (possibly compromised) method still active.
        account.resetMfaForRecovery(clock.instant());
        accounts.save(account);
        return toView(account);
    }

    @Override
    @Transactional
    public void recoverPassword(String email, String recoveryCode, String newPassword) {
        Optional<Account> found = accounts.findByEmailIgnoreCase(email);
        if (found.isEmpty()) {
            // Approximate the cost of scanning a full set of unused codes with no match,
            // so an unknown email isn't measurably faster than a known one with none matching.
            for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
                passwordEncoder.matches(recoveryCode, DUMMY_BCRYPT_HASH);
            }
            return;
        }
        Account account = found.get();
        Optional<RecoveryCode> matched = findMatchingUnusedCode(account.getId(), recoveryCode);
        if (matched.isEmpty()) {
            return;
        }
        matched.get().consume(clock.instant());
        recoveryCodeRepository.save(matched.get());
        // Does not touch mfaMethod: if the authenticator is still available, the next login
        // challenges as usual — only a *lost second factor* (verifyRecoveryCode) forces re-enrollment.
        account.changePasswordHash(passwordEncoder.encode(newPassword), clock.instant());
        accounts.save(account);
    }

    @Override
    @Transactional
    public AccountView resetMfa(UUID accountId) {
        Account account = require(accountId);
        account.resetMfaForRecovery(clock.instant());
        accounts.save(account);
        // A recovery code the wrong person holds must stop working immediately, not just the
        // method flag — otherwise it could still be spent via recoverPassword/verifyRecoveryCode
        // after an admin has "fixed" this account.
        recoveryCodeRepository.deleteAllByAccountId(accountId);
        return toView(account);
    }

    private List<String> issueRecoveryCodes(UUID accountId) {
        recoveryCodeRepository.deleteAllByAccountId(accountId);
        List<String> plaintext = recoveryCodeGenerator.generateCodes(RECOVERY_CODE_COUNT);
        Instant now = clock.instant();
        recoveryCodeRepository.saveAll(plaintext.stream()
                .map(code -> RecoveryCode.create(ids.newId(), accountId, passwordEncoder.encode(code), now))
                .toList());
        return plaintext;
    }

    private Optional<RecoveryCode> findMatchingUnusedCode(UUID accountId, String rawCode) {
        return recoveryCodeRepository.findUnusedByAccountId(accountId).stream()
                .filter(recoveryCode -> passwordEncoder.matches(rawCode, recoveryCode.getCodeHash()))
                .findFirst();
    }

    private Account require(UUID accountId) {
        return accounts.findById(accountId).orElseThrow(() -> new NotFoundException("Account not found"));
    }

    private static AccountView toView(Account account) {
        return new AccountView(account.getId(), account.getEmail(), account.getRole(), account.isEnabled(),
                account.getTokenVersion(), account.getMfaMethod(), account.getCreatedAt());
    }
}
