package com.campaignorganizer.accounts.application.mfa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.accounts.application.account.port.out.AccountRepositoryPort;
import com.campaignorganizer.accounts.application.account.port.out.TotpPort;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.application.mfa.port.in.MfaResults.MfaEnrollmentOutcome;
import com.campaignorganizer.accounts.application.mfa.port.in.MfaResults.RecoveryCodeStatus;
import com.campaignorganizer.accounts.application.mfa.port.in.MfaResults.TotpSetupStart;
import com.campaignorganizer.accounts.application.mfa.port.out.RecoveryCodePort;
import com.campaignorganizer.accounts.application.mfa.port.out.RecoveryCodeRepositoryPort;
import com.campaignorganizer.accounts.application.mfa.port.out.WebAuthnCredentialRepositoryPort;
import com.campaignorganizer.accounts.domain.account.Account;
import com.campaignorganizer.accounts.domain.account.MfaMethod;
import com.campaignorganizer.accounts.domain.account.Role;
import com.campaignorganizer.accounts.domain.recoverycode.RecoveryCode;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.AuthenticationFailedException;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Application service unit test for MFA setup/challenge/recovery with mocked out-ports.
 * Uses real {@link PasswordEncoder}/{@link TextEncryptor} instances rather than mocks — the
 * round-trip through real BCrypt/AES-GCM is cheap and worth actually exercising.
 */
@ExtendWith(MockitoExtension.class)
class MfaServiceTest {

    @Mock
    private AccountRepositoryPort accounts;
    @Mock
    private RecoveryCodeRepositoryPort recoveryCodeRepository;
    @Mock
    private TotpPort totp;
    @Mock
    private RecoveryCodePort recoveryCodeGenerator;
    @Mock
    private IdGenerator ids;
    @Mock
    private WebAuthnCredentialRepositoryPort webAuthnCredentials;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final TextEncryptor textEncryptor = Encryptors.delux("unit-test-key", "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4");
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);

    private MfaService service;
    private final UUID accountId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new MfaService(accounts, recoveryCodeRepository, totp, recoveryCodeGenerator, passwordEncoder,
                textEncryptor, webAuthnCredentials, ids, clock);
    }

    @Test
    void startTotpSetupStoresEncryptedPendingSecret() {
        Account account = freshAccount();
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));
        when(totp.generateSecret()).thenReturn("SECRET123");
        when(totp.provisioningUri("SECRET123", account.getEmail())).thenReturn("otpauth://totp/uri");
        when(totp.qrCodeDataUri("SECRET123", account.getEmail())).thenReturn("data:image/png;base64,abc");

        TotpSetupStart result = service.startTotpSetup(accountId);

        assertThat(result.secret()).isEqualTo("SECRET123");
        assertThat(result.provisioningUri()).isEqualTo("otpauth://totp/uri");
        assertThat(result.qrCodeDataUri()).isEqualTo("data:image/png;base64,abc");
        assertThat(account.getMfaMethod()).isEqualTo(MfaMethod.NONE);
        assertThat(textEncryptor.decrypt(account.getTotpSecretPendingEncrypted())).isEqualTo("SECRET123");
        verify(accounts).save(account);
    }

    @Test
    void startTotpSetupFailsWhenAlreadyEnrolled() {
        Account account = enrolledAccount("SECRET123");
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));
        when(totp.generateSecret()).thenReturn("ANOTHER");

        assertThatThrownBy(() -> service.startTotpSetup(accountId)).isInstanceOf(ValidationException.class);
    }

    @Test
    void confirmTotpSetupWithCorrectCodeActivatesMethodAndIssuesRecoveryCodes() {
        Account account = freshAccount();
        account.beginTotpEnrollment(textEncryptor.encrypt("SECRET123"), clock.instant());
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));
        when(totp.verifyCode("SECRET123", "111111")).thenReturn(true);
        when(recoveryCodeGenerator.generateCodes(10)).thenReturn(List.of("code-1", "code-2"));
        when(ids.newId()).thenReturn(UUID.randomUUID(), UUID.randomUUID());

        MfaEnrollmentOutcome outcome = service.confirmTotpSetup(accountId, "111111");

        assertThat(account.getMfaMethod()).isEqualTo(MfaMethod.TOTP);
        assertThat(account.getTotpSecretPendingEncrypted()).isNull();
        assertThat(outcome.account().mfaMethod()).isEqualTo(MfaMethod.TOTP);
        assertThat(outcome.recoveryCodes()).containsExactly("code-1", "code-2");
        verify(recoveryCodeRepository).deleteAllByAccountId(accountId);
        verify(recoveryCodeRepository).saveAll(any());
    }

    @Test
    void confirmTotpSetupWithWrongCodeFailsAndLeavesEnrollmentPending() {
        Account account = freshAccount();
        account.beginTotpEnrollment(textEncryptor.encrypt("SECRET123"), clock.instant());
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));
        when(totp.verifyCode("SECRET123", "000000")).thenReturn(false);

        assertThatThrownBy(() -> service.confirmTotpSetup(accountId, "000000"))
                .isInstanceOf(AuthenticationFailedException.class);

        assertThat(account.getMfaMethod()).isEqualTo(MfaMethod.NONE);
        assertThat(account.getTotpSecretPendingEncrypted()).isNotNull();
        verify(recoveryCodeRepository, never()).saveAll(any());
    }

    @Test
    void confirmTotpSetupFailsWithoutAPendingEnrollment() {
        Account account = freshAccount();
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.confirmTotpSetup(accountId, "111111"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void confirmWebauthnSetupActivatesMethodAndIssuesRecoveryCodesWhenCredentialExists() {
        Account account = freshAccount();
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));
        when(webAuthnCredentials.existsByAccountId(accountId)).thenReturn(true);
        when(recoveryCodeGenerator.generateCodes(10)).thenReturn(List.of("code-1", "code-2"));
        when(ids.newId()).thenReturn(UUID.randomUUID(), UUID.randomUUID());

        MfaEnrollmentOutcome outcome = service.confirmWebauthnSetup(accountId);

        assertThat(account.getMfaMethod()).isEqualTo(MfaMethod.WEBAUTHN);
        assertThat(outcome.account().mfaMethod()).isEqualTo(MfaMethod.WEBAUTHN);
        assertThat(outcome.recoveryCodes()).containsExactly("code-1", "code-2");
        verify(recoveryCodeRepository).saveAll(any());
    }

    @Test
    void confirmWebauthnSetupFailsWithoutAStoredCredential() {
        Account account = freshAccount();
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));
        when(webAuthnCredentials.existsByAccountId(accountId)).thenReturn(false);

        assertThatThrownBy(() -> service.confirmWebauthnSetup(accountId)).isInstanceOf(ValidationException.class);

        assertThat(account.getMfaMethod()).isEqualTo(MfaMethod.NONE);
    }

    @Test
    void verifyTotpChallengeSucceedsWithCorrectCode() {
        Account account = enrolledAccount("SECRET123");
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));
        when(totp.verifyCode("SECRET123", "111111")).thenReturn(true);

        AccountView result = service.verifyTotpChallenge(accountId, "111111");

        assertThat(result.mfaMethod()).isEqualTo(MfaMethod.TOTP);
    }

    @Test
    void verifyTotpChallengeFailsWithWrongCode() {
        Account account = enrolledAccount("SECRET123");
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));
        when(totp.verifyCode("SECRET123", "000000")).thenReturn(false);

        assertThatThrownBy(() -> service.verifyTotpChallenge(accountId, "000000"))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void verifyRecoveryCodeConsumesMatchingCodeAndResetsMfaMethod() {
        Account account = enrolledAccount("SECRET123");
        String hash = passwordEncoder.encode("recovery-code-1");
        RecoveryCode code = RecoveryCode.create(UUID.randomUUID(), accountId, hash, clock.instant());
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));
        when(recoveryCodeRepository.findUnusedByAccountId(accountId)).thenReturn(List.of(code));

        AccountView result = service.verifyRecoveryCode(accountId, "recovery-code-1");

        assertThat(result.mfaMethod()).isEqualTo(MfaMethod.NONE);
        assertThat(account.getMfaMethod()).isEqualTo(MfaMethod.NONE);
        assertThat(code.isUsed()).isTrue();
        verify(recoveryCodeRepository).save(code);
    }

    @Test
    void verifyRecoveryCodeFailsWithNoMatchingCode() {
        Account account = enrolledAccount("SECRET123");
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));
        when(recoveryCodeRepository.findUnusedByAccountId(accountId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.verifyRecoveryCode(accountId, "anything"))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void recoverPasswordChangesPasswordAndConsumesCodeWithoutTouchingMfaMethod() {
        Account account = enrolledAccount("SECRET123");
        String hash = passwordEncoder.encode("recovery-code-1");
        RecoveryCode code = RecoveryCode.create(UUID.randomUUID(), accountId, hash, clock.instant());
        when(accounts.findByEmailIgnoreCase("gm@example.com")).thenReturn(Optional.of(account));
        when(recoveryCodeRepository.findUnusedByAccountId(accountId)).thenReturn(List.of(code));
        String oldHash = account.getPasswordHash();

        service.recoverPassword("gm@example.com", "recovery-code-1", "brand-new-password12");

        assertThat(account.getPasswordHash()).isNotEqualTo(oldHash);
        assertThat(account.getMfaMethod()).isEqualTo(MfaMethod.TOTP);
        assertThat(code.isUsed()).isTrue();
        verify(accounts).save(account);
    }

    @Test
    void recoverPasswordWithUnknownEmailChangesNothing() {
        when(accounts.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        service.recoverPassword("nobody@example.com", "any-code", "brand-new-password12");

        verify(accounts, never()).save(any());
    }

    @Test
    void recoverPasswordWithWrongCodeChangesNothing() {
        Account account = enrolledAccount("SECRET123");
        when(accounts.findByEmailIgnoreCase("gm@example.com")).thenReturn(Optional.of(account));
        when(recoveryCodeRepository.findUnusedByAccountId(accountId)).thenReturn(List.of());
        String oldHash = account.getPasswordHash();

        service.recoverPassword("gm@example.com", "wrong-code", "brand-new-password12");

        assertThat(account.getPasswordHash()).isEqualTo(oldHash);
        verify(accounts, never()).save(any());
    }

    /** Admin recovery path (ADR-0111) for an enrollment the legitimate owner can't clear themselves. */
    @Test
    void resetMfaClearsMethodAndSecretAndDeletesOutstandingRecoveryCodesAndWebauthnCredential() {
        Account account = enrolledAccount("SECRET123");
        int versionBefore = account.getTokenVersion();
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));

        AccountView result = service.resetMfa(accountId);

        assertThat(result.mfaMethod()).isEqualTo(MfaMethod.NONE);
        assertThat(account.getMfaMethod()).isEqualTo(MfaMethod.NONE);
        assertThat(account.getTotpSecretEncrypted()).isNull();
        assertThat(account.getTokenVersion()).isEqualTo(versionBefore + 1);
        verify(recoveryCodeRepository).deleteAllByAccountId(accountId);
        verify(webAuthnCredentials).deleteByAccountId(accountId);
    }

    @Test
    void getRecoveryCodeStatusReturnsTheUnusedCount() {
        when(recoveryCodeRepository.countUnusedByAccountId(accountId)).thenReturn(7);

        RecoveryCodeStatus status = service.getRecoveryCodeStatus(accountId);

        assertThat(status.remaining()).isEqualTo(7);
    }

    @Test
    void regenerateRecoveryCodesIssuesAFreshBatch() {
        Account account = enrolledAccount("SECRET123");
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));
        when(recoveryCodeGenerator.generateCodes(10)).thenReturn(List.of("new-1", "new-2"));
        when(ids.newId()).thenReturn(UUID.randomUUID(), UUID.randomUUID());

        List<String> codes = service.regenerateRecoveryCodes(accountId);

        assertThat(codes).containsExactly("new-1", "new-2");
        verify(recoveryCodeRepository).deleteAllByAccountId(accountId);
        verify(recoveryCodeRepository).saveAll(any());
    }

    @Test
    void regenerateRecoveryCodesFailsForAnUnknownAccount() {
        when(accounts.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.regenerateRecoveryCodes(accountId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void startTotpReEnrollmentStoresEncryptedPendingSecretForAnAlreadyTotpAccount() {
        Account account = enrolledAccount("OLD-SECRET");
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));
        when(totp.generateSecret()).thenReturn("NEW-SECRET");
        when(totp.provisioningUri("NEW-SECRET", account.getEmail())).thenReturn("otpauth://totp/uri");
        when(totp.qrCodeDataUri("NEW-SECRET", account.getEmail())).thenReturn("data:image/png;base64,abc");

        TotpSetupStart result = service.startTotpReEnrollment(accountId);

        assertThat(result.secret()).isEqualTo("NEW-SECRET");
        assertThat(account.getMfaMethod()).isEqualTo(MfaMethod.TOTP);
        assertThat(textEncryptor.decrypt(account.getTotpSecretPendingEncrypted())).isEqualTo("NEW-SECRET");
        verify(accounts).save(account);
    }

    @Test
    void startTotpReEnrollmentFailsWhenNoTotpMethodIsActiveYet() {
        Account account = freshAccount();
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));
        when(totp.generateSecret()).thenReturn("NEW-SECRET");

        assertThatThrownBy(() -> service.startTotpReEnrollment(accountId)).isInstanceOf(ValidationException.class);
    }

    @Test
    void confirmTotpReEnrollmentWithCorrectCodeSwapsTheActiveSecretWithoutReissuingRecoveryCodes() {
        Account account = enrolledAccount("OLD-SECRET");
        account.beginTotpReEnrollment(textEncryptor.encrypt("NEW-SECRET"), clock.instant());
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));
        when(totp.verifyCode("NEW-SECRET", "222222")).thenReturn(true);

        service.confirmTotpReEnrollment(accountId, "222222");

        assertThat(account.getMfaMethod()).isEqualTo(MfaMethod.TOTP);
        assertThat(textEncryptor.decrypt(account.getTotpSecretEncrypted())).isEqualTo("NEW-SECRET");
        assertThat(account.getTotpSecretPendingEncrypted()).isNull();
        verify(recoveryCodeRepository, never()).saveAll(any());
    }

    @Test
    void confirmTotpReEnrollmentWithWrongCodeFailsAndLeavesTheOldSecretActive() {
        Account account = enrolledAccount("OLD-SECRET");
        account.beginTotpReEnrollment(textEncryptor.encrypt("NEW-SECRET"), clock.instant());
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));
        when(totp.verifyCode("NEW-SECRET", "000000")).thenReturn(false);

        assertThatThrownBy(() -> service.confirmTotpReEnrollment(accountId, "000000"))
                .isInstanceOf(ValidationException.class);

        assertThat(textEncryptor.decrypt(account.getTotpSecretEncrypted())).isEqualTo("OLD-SECRET");
    }

    @Test
    void confirmTotpReEnrollmentFailsWithoutAPendingReEnrollment() {
        Account account = enrolledAccount("OLD-SECRET");
        when(accounts.findById(accountId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.confirmTotpReEnrollment(accountId, "111111"))
                .isInstanceOf(ValidationException.class);
    }

    private Account freshAccount() {
        return Account.reconstitute(accountId, "gm@example.com", "hash", Role.USER, true, 0, 0, null,
                MfaMethod.NONE, null, null, clock.instant(), clock.instant());
    }

    private Account enrolledAccount(String secret) {
        Account account = freshAccount();
        account.beginTotpEnrollment(textEncryptor.encrypt(secret), clock.instant());
        account.completeTotpEnrollment(clock.instant());
        return account;
    }
}
