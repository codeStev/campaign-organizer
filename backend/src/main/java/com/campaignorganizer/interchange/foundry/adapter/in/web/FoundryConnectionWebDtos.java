package com.campaignorganizer.interchange.foundry.adapter.in.web;

import java.util.List;

/** Web request/response models for {@code /api/worlds/{worldId}/foundry-connection}.
 * None of these ever carries the API key (plaintext or encrypted) — only
 * {@link FoundryConnectionRequest#apiKey()} accepts one on the way in, and a
 * blank value there means "keep the currently stored key." */
public final class FoundryConnectionWebDtos {

    private FoundryConnectionWebDtos() {
    }

    public record FoundryConnectionRequest(String relayBaseUrl, String clientId, String apiKey) {
    }

    public record FoundryConnectionResponse(String relayBaseUrl, String clientId, boolean configured) {
    }

    public record FoundryConnectionTestResponse(boolean ok, List<String> connectedClientIds, String error) {
    }
}
