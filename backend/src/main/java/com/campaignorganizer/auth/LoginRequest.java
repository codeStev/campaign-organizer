package com.campaignorganizer.auth;

import jakarta.validation.constraints.NotBlank;

/** Body of {@code POST /api/auth/login}. */
public record LoginRequest(@NotBlank String password) {
}
