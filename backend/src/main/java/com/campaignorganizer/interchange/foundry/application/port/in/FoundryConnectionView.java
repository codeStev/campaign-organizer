package com.campaignorganizer.interchange.foundry.application.port.in;

import java.util.UUID;

/** Read model for a world's Foundry connection — never carries the API key, even encrypted. */
public record FoundryConnectionView(UUID worldId, String relayBaseUrl, String clientId, boolean configured) {
}
