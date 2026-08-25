package com.campaignorganizer.ai.adapter.in.web;

/** Result of the Settings "test this provider" button; {@code error} null when ok. */
public record AiProviderTestResponse(boolean ok, String model, long latencyMs, String error) {
}
