package com.campaignorganizer.accounts.adapter.account.out.oidc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Any OAuth2 failure (denied consent, state mismatch, provider error) redirects back to the
 * frontend with a generic reason — no failure detail (which could echo provider-supplied text)
 * ever lands in a URL (ADR-0113).
 */
@Component
public class OidcAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws java.io.IOException {
        response.sendRedirect(OidcRedirect.error("login_failed"));
    }
}
