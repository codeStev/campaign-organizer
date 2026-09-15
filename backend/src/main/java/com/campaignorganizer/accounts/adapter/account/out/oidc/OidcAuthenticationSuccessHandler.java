package com.campaignorganizer.accounts.adapter.account.out.oidc;

import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.application.oidc.port.in.AuthenticateOrRegisterViaOidcUseCase;
import com.campaignorganizer.accounts.application.oidc.port.out.OidcLoginExchangePort;
import com.campaignorganizer.accounts.domain.account.MfaMethod;
import com.campaignorganizer.auth.LoginResponse;
import com.campaignorganizer.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Resolves the Google-verified identity to a local account and hands the resulting {@link
 * LoginResponse} to the browser (ADR-0113) — Google sign-in still only proves the first factor;
 * this app's own mandatory MFA (ADR-0111) isn't bypassed, so the response shape and the token's
 * factor claim are identical to {@code AuthController.login}'s.
 *
 * <p>Unlike {@code WebAuthnAuthenticationSuccessHandler}, this can't write JSON directly onto the
 * response: {@code oauth2Login()} ends in a full-page browser redirect, not an XHR. Staging the
 * response behind a single-use {@link OidcLoginExchangePort} code and redirecting with just that
 * code keeps the bearer token itself out of the URL entirely — never in browser history, server
 * access logs, or a Referer header.
 */
@Component
public class OidcAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Duration EXCHANGE_TTL = Duration.ofMinutes(2);

    private final AuthenticateOrRegisterViaOidcUseCase authenticateOrRegister;
    private final OidcLoginExchangePort exchanges;
    private final JwtService jwtService;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public OidcAuthenticationSuccessHandler(AuthenticateOrRegisterViaOidcUseCase authenticateOrRegister,
                                            OidcLoginExchangePort exchanges, JwtService jwtService, Clock clock) {
        this.authenticateOrRegister = authenticateOrRegister;
        this.exchanges = exchanges;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws java.io.IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OidcUser oidcUser = (OidcUser) oauthToken.getPrincipal();
        if (!Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
            response.sendRedirect(OidcRedirect.error("email_not_verified"));
            return;
        }
        AccountView account = authenticateOrRegister.authenticateOrRegister("GOOGLE", oidcUser.getSubject(),
                oidcUser.getEmail());
        JwtService.IssuedToken issued = jwtService.issue(account.id(), account.role(), account.tokenVersion(),
                Set.of(JwtService.PASSWORD_FACTOR));
        LoginResponse loginResponse = account.mfaMethod() == MfaMethod.NONE
                ? LoginResponse.setupRequired(issued.token(), issued.expiresAt())
                : LoginResponse.challengeRequired(issued.token(), issued.expiresAt(), account.mfaMethod());
        UUID code = exchanges.stage(writeJson(loginResponse), clock.instant().plus(EXCHANGE_TTL));
        response.sendRedirect(OidcRedirect.success(code));
    }

    private String writeJson(LoginResponse loginResponse) {
        try {
            return objectMapper.writeValueAsString(loginResponse);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize LoginResponse", ex);
        }
    }
}
