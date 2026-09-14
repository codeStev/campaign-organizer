package com.campaignorganizer.auth;

import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.application.account.port.published.AuthenticateAccountPort;
import com.campaignorganizer.accounts.domain.account.MfaMethod;
import com.campaignorganizer.security.JwtService;
import jakarta.validation.Valid;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Login only — see {@code accounts.adapter.account.in.web.RegisterController}
 * for registration, {@code accounts.adapter.account.in.web.AccountAdminController}
 * for account management, and {@code accounts.adapter.account.in.web.MfaController}
 * for completing the second factor this endpoint always leaves outstanding.
 * Deliberately returns the same generic 401 for a wrong password, an unknown
 * email, and a disabled or locked account, so none of those are
 * distinguishable (ADR-0110, anti-enumeration). A correct password never
 * grants full access on its own (ADR-0111, mandatory MFA) — the returned
 * token only carries the PASSWORD factor, and {@code status} tells the
 * caller whether it still needs to complete MFA setup or clear a challenge.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticateAccountPort authenticateAccountPort;
    private final JwtService jwtService;

    public AuthController(AuthenticateAccountPort authenticateAccountPort, JwtService jwtService) {
        this.authenticateAccountPort = authenticateAccountPort;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AccountView account = authenticateAccountPort.authenticate(request.email(), request.password())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        JwtService.IssuedToken issued = jwtService.issue(account.id(), account.role(), account.tokenVersion(),
                Set.of(JwtService.PASSWORD_FACTOR));
        if (account.mfaMethod() == MfaMethod.NONE) {
            return LoginResponse.setupRequired(issued.token(), issued.expiresAt());
        }
        return LoginResponse.challengeRequired(issued.token(), issued.expiresAt(), account.mfaMethod());
    }
}
