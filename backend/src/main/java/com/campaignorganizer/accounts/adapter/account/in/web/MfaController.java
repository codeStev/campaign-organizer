package com.campaignorganizer.accounts.adapter.account.in.web;

import com.campaignorganizer.accounts.adapter.account.in.web.MfaWebDtos.MfaCodeRequest;
import com.campaignorganizer.accounts.adapter.account.in.web.MfaWebDtos.MfaEnrollmentResult;
import com.campaignorganizer.accounts.adapter.account.in.web.MfaWebDtos.RecoveryCodeRequest;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.application.mfa.port.in.ConfirmTotpSetupUseCase;
import com.campaignorganizer.accounts.application.mfa.port.in.MfaResults.MfaEnrollmentOutcome;
import com.campaignorganizer.accounts.application.mfa.port.in.MfaResults.TotpSetupStart;
import com.campaignorganizer.accounts.application.mfa.port.in.StartTotpSetupUseCase;
import com.campaignorganizer.accounts.application.mfa.port.in.VerifyRecoveryCodeUseCase;
import com.campaignorganizer.accounts.application.mfa.port.in.VerifyTotpChallengeUseCase;
import com.campaignorganizer.auth.LoginResponse;
import com.campaignorganizer.auth.TokenResponse;
import com.campaignorganizer.security.CurrentUserPort;
import com.campaignorganizer.security.JwtService;
import jakarta.validation.Valid;
import java.util.Set;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TOTP setup/challenge and recovery-code endpoints (ADR-0111). Every endpoint here requires a
 * token carrying at least the PASSWORD factor — see
 * {@code com.campaignorganizer.config.SecurityConfig}'s {@code /api/auth/mfa/**} matcher, which
 * deliberately doesn't also require the MFA factor (completing it is the whole point). See
 * {@link RecoverPasswordController} for the one MFA-related endpoint that needs no token at
 * all, and WebAuthn's equivalent endpoints, which are Spring Security's own, not defined here.
 */
@RestController
@RequestMapping("/api/auth/mfa")
public class MfaController {

    private final StartTotpSetupUseCase startTotpSetupUseCase;
    private final ConfirmTotpSetupUseCase confirmTotpSetupUseCase;
    private final VerifyTotpChallengeUseCase verifyTotpChallengeUseCase;
    private final VerifyRecoveryCodeUseCase verifyRecoveryCodeUseCase;
    private final CurrentUserPort currentUser;
    private final JwtService jwtService;

    public MfaController(StartTotpSetupUseCase startTotpSetupUseCase,
                         ConfirmTotpSetupUseCase confirmTotpSetupUseCase,
                         VerifyTotpChallengeUseCase verifyTotpChallengeUseCase,
                         VerifyRecoveryCodeUseCase verifyRecoveryCodeUseCase, CurrentUserPort currentUser,
                         JwtService jwtService) {
        this.startTotpSetupUseCase = startTotpSetupUseCase;
        this.confirmTotpSetupUseCase = confirmTotpSetupUseCase;
        this.verifyTotpChallengeUseCase = verifyTotpChallengeUseCase;
        this.verifyRecoveryCodeUseCase = verifyRecoveryCodeUseCase;
        this.currentUser = currentUser;
        this.jwtService = jwtService;
    }

    @PostMapping("/setup/totp/start")
    public TotpSetupStart startTotpSetup() {
        return startTotpSetupUseCase.startTotpSetup(currentUser.currentAccountId());
    }

    @PostMapping("/setup/totp/confirm")
    public MfaEnrollmentResult confirmTotpSetup(@Valid @RequestBody MfaCodeRequest request) {
        MfaEnrollmentOutcome outcome =
                confirmTotpSetupUseCase.confirmTotpSetup(currentUser.currentAccountId(), request.code());
        JwtService.IssuedToken issued = issueFullToken(outcome.account());
        return new MfaEnrollmentResult(issued.token(), "Bearer", issued.expiresAt(), outcome.recoveryCodes());
    }

    @PostMapping("/verify")
    public TokenResponse verifyTotpChallenge(@Valid @RequestBody MfaCodeRequest request) {
        AccountView account = verifyTotpChallengeUseCase.verifyTotpChallenge(currentUser.currentAccountId(),
                request.code());
        JwtService.IssuedToken issued = issueFullToken(account);
        return TokenResponse.bearer(issued.token(), issued.expiresAt());
    }

    @PostMapping("/verify-recovery-code")
    public LoginResponse verifyRecoveryCode(@Valid @RequestBody RecoveryCodeRequest request) {
        AccountView account = verifyRecoveryCodeUseCase.verifyRecoveryCode(currentUser.currentAccountId(),
                request.recoveryCode());
        // Always MFA_SETUP_REQUIRED: resetMfaForRecovery already cleared the account's method.
        JwtService.IssuedToken issued = jwtService.issue(account.id(), account.role(), account.tokenVersion(),
                Set.of(JwtService.PASSWORD_FACTOR));
        return LoginResponse.setupRequired(issued.token(), issued.expiresAt());
    }

    private JwtService.IssuedToken issueFullToken(AccountView account) {
        return jwtService.issue(account.id(), account.role(), account.tokenVersion());
    }
}
