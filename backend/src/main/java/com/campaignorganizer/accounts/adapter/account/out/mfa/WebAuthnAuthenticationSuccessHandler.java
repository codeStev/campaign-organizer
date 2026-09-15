package com.campaignorganizer.accounts.adapter.account.out.mfa;

import com.campaignorganizer.accounts.application.account.port.published.AccountQueryPort;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.domain.account.MfaMethod;
import com.campaignorganizer.auth.TokenResponse;
import com.campaignorganizer.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
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
 *
 * <p>Deliberately checks {@code account.mfaMethod() == WEBAUTHN} before issuing a full token —
 * found missing during this feature's own {@code security-review} pass. Without it, an attacker
 * who only has the account's leaked password (not its real, already-active second factor) could
 * silently register their own passkey via {@code POST /webauthn/register} — nothing gates that
 * write on the account's current {@code mfaMethod}, only on holding *some* valid PASSWORD-factor
 * token — and use it to log in indefinitely, fully bypassing whatever second factor the account's
 * legitimate owner actually has configured. {@code verifyTotpChallenge} already has the same kind
 * of check on the TOTP side; this mirrors it on the WebAuthn side.
 *
 * <p>Registers {@link JavaTimeModule} explicitly on its own {@link ObjectMapper} instance — this
 * app has no directly-injectable {@code ObjectMapper} bean (Boot's Jackson autoconfiguration
 * wires MVC's message converter some other way; confirmed the hard way, via a context-loading
 * failure, that no such bean is exposed to plain {@code @Autowired}). A bare {@code new
 * ObjectMapper()} has no date/time module registered at all: it would throw serializing {@link
 * TokenResponse}'s {@code Instant expiresAt} field on every successful login (caught by this
 * class's own unit test).
 */
@Component
public class WebAuthnAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final AccountQueryPort accounts;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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
        if (account.mfaMethod() != MfaMethod.WEBAUTHN) {
            throw new AccessDeniedException("WebAuthn is not this account's active MFA method");
        }
        JwtService.IssuedToken issued = jwtService.issue(account.id(), account.role(), account.tokenVersion());
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), TokenResponse.bearer(issued.token(), issued.expiresAt()));
    }
}
