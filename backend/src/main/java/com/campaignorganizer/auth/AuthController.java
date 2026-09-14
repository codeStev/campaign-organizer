package com.campaignorganizer.auth;

import com.campaignorganizer.accounts.application.account.port.published.AuthenticateAccountPort;
import com.campaignorganizer.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Login only — see {@code accounts.adapter.account.in.web.RegisterController}
 * for registration and {@code accounts.adapter.account.in.web.AccountAdminController}
 * for account management. Deliberately returns the same generic 401 for a
 * wrong password, an unknown email, and a disabled or locked account, so
 * none of those are distinguishable (ADR-0110, anti-enumeration).
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
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        var account = authenticateAccountPort.authenticate(request.email(), request.password())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        JwtService.IssuedToken issued = jwtService.issue(account.id(), account.role(), account.tokenVersion());
        return TokenResponse.bearer(issued.token(), issued.expiresAt());
    }
}
