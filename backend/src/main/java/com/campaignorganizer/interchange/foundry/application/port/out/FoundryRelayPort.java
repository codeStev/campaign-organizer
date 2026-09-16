package com.campaignorganizer.interchange.foundry.application.port.out;

import java.util.List;

/**
 * Outbound port to the self-hosted Foundry relay (ADR-0115) — a third-party
 * generic HTTP+JSON bridge in front of a live Foundry VTT session. Only the
 * connection-test primitive exists so far; document upsert/upload methods
 * are added alongside the push use cases that need them.
 */
public interface FoundryRelayPort {

    /** Which Foundry session a call targets, and the credentials to reach the relay with. */
    record Credentials(String relayBaseUrl, String apiKey, String clientId) {
    }

    /** {@code clientId}s of every Foundry session currently connected to the relay. */
    List<String> listConnectedClients(Credentials credentials);
}
