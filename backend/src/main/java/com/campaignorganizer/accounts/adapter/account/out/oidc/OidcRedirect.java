package com.campaignorganizer.accounts.adapter.account.out.oidc;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * The relative redirect URLs the OIDC success/failure handlers send the browser back to
 * (ADR-0113) — always relative, never an absolute URL: this app is always same-origin
 * (ADR-0059's combined image), so no frontend-base-URL config is needed.
 */
final class OidcRedirect {

    private OidcRedirect() {
    }

    static String success(UUID exchangeCode) {
        return "/?code=" + exchangeCode;
    }

    static String error(String reason) {
        return "/?oidcError=" + URLEncoder.encode(reason, StandardCharsets.UTF_8);
    }
}
