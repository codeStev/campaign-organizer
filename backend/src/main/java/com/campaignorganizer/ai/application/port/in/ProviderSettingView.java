package com.campaignorganizer.ai.application.port.in;

/**
 * A provider setting combined with what the {@code TextGenerationPort} itself
 * reports (default model, whether it's actually configured) — the Settings UI
 * needs both to explain why a provider might not get used despite its priority.
 */
public record ProviderSettingView(String providerId, String model, String defaultModel, boolean configured,
        int priority) {
}
