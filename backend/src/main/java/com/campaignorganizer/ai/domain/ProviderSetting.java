package com.campaignorganizer.ai.domain;

import com.campaignorganizer.shared.domain.ValidationException;

/**
 * Which model a provider uses, and its try-order relative to other providers
 * (lower {@code priority} tried first). {@code model} null means "use that
 * provider's built-in default" ({@code TextGenerationPort#defaultModel()}).
 * See ADR-0065.
 */
public record ProviderSetting(String providerId, String model, int priority) {

    public ProviderSetting {
        if (providerId == null || providerId.isBlank()) {
            throw new ValidationException("Provider id must not be blank");
        }
        if (model != null && model.isBlank()) {
            model = null;
        }
    }
}
