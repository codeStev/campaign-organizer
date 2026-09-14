package com.campaignorganizer.auth;

/**
 * A correct password never grants full access on its own (ADR-0111, mandatory MFA) — see
 * {@link LoginResponse}.
 */
public enum LoginStatus {
    MFA_SETUP_REQUIRED,
    MFA_CHALLENGE_REQUIRED
}
