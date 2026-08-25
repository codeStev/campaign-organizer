package com.campaignorganizer.ai.adapter.in.web;

public record AiProviderSettingResponse(
        String providerId, String model, String defaultModel, boolean configured, int priority) {
}
