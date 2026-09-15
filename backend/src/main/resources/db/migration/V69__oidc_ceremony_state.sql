-- SSO/OIDC login (ADR-0113): stateless replacements for the two pieces of server-held state
-- Spring's oauth2Login() DSL otherwise keeps in HttpSession, which this app has none of.
--
-- oidc_authorization_requests backs a stateless AuthorizationRequestRepository, keyed by the
-- OAuth `state` param — unlike webauthn_challenges (keyed by account_id, since every WebAuthn
-- ceremony already has a PASSWORD-factor token), an initial Google login has no account/token
-- yet, so state is the only available correlation key.
--
-- oidc_login_exchanges is the single-use handoff between the OIDC success handler (which can
-- only respond with a browser redirect, not a JSON body) and the frontend's exchange call —
-- staged with a short TTL, deleted the moment it's redeemed.

CREATE TABLE oidc_authorization_requests (
    state        VARCHAR(200) PRIMARY KEY,
    request_json TEXT NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL
);

CREATE TABLE oidc_login_exchanges (
    code                UUID PRIMARY KEY,
    login_response_json TEXT NOT NULL,
    expires_at          TIMESTAMPTZ NOT NULL
);
