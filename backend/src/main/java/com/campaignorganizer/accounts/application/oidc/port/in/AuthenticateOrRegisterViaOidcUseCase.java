package com.campaignorganizer.accounts.application.oidc.port.in;

import com.campaignorganizer.accounts.application.account.port.published.AccountView;

/**
 * Resolves a verified external identity (Google only for now) to a local account (ADR-0113):
 * returns the existing account on a repeat login, creates a new one (applying the same
 * first-registrant-becomes-ADMIN rule as password self-registration) on a genuinely new
 * identity, and refuses — never silently links — when the email already belongs to a
 * differently-identified account.
 */
public interface AuthenticateOrRegisterViaOidcUseCase {

    /**
     * @param authProvider e.g. {@code "GOOGLE"}
     * @param externalSubject the provider's own stable, opaque user id (the OIDC {@code sub} claim)
     * @param email the provider-verified email, used only to create a new account or detect a
     *     conflict with an existing differently-identified one — never to look up a match
     * @throws com.campaignorganizer.shared.domain.ConflictException if {@code email} already
     *     belongs to a different account (password-based, or a different external identity)
     */
    AccountView authenticateOrRegister(String authProvider, String externalSubject, String email);
}
