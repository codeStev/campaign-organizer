package com.campaignorganizer.accounts.adapter.account.out.mfa;

import com.campaignorganizer.accounts.application.account.port.published.AccountQueryPort;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.auth.TokenResponse;
import com.campaignorganizer.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthentication;
import org.springframework.stereotype.Component;

/**
 * Replaces {@code WebAuthnAuthenticationFilter}'s default success behavior — writing Spring's
 * own {@code HttpMessageConverterAuthenticationSuccessHandler} output — with this app's own
 * bearer token (ADR-0111 follow-up): the same {@link TokenResponse} shape {@code MfaController}
 * already returns after a TOTP challenge, carrying both the PASSWORD and MFA factors, since a
 * successful WebAuthn ceremony IS proof of the second factor. Wired onto the actual filter
 * instance in {@code SecurityConfig} after {@code http.build()} — {@code WebAuthnConfigurer}
 * exposes no DSL hook for this (confirmed by reading its source directly).
 */
@Component
public class WebAuthnAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final AccountQueryPort accounts;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebAuthnAuthenticationSuccessHandler(JwtService jwtService, AccountQueryPort accounts) {
        this.jwtService = jwtService;
        this.accounts = accounts;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws java.io.IOException {
        WebAuthnAuthentication webAuthnAuthentication = (WebAuthnAuthentication) authentication;
        UUID accountId = WebAuthnUserHandle.toAccountId(webAuthnAuthentication.getPrincipal().getId());
        AccountView account = accounts.findById(accountId)
                .orElseThrow(() -> new IllegalStateException("WebAuthn-authenticated account no longer exists"));
        JwtService.IssuedToken issued = jwtService.issue(account.id(), account.role(), account.tokenVersion());
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), TokenResponse.bearer(issued.token(), issued.expiresAt()));
    }
}
