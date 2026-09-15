package com.campaignorganizer.auth;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Body of {@code POST /api/auth/oidc/exchange} (ADR-0113). */
public record OidcExchangeRequest(@NotNull UUID code) {
}
