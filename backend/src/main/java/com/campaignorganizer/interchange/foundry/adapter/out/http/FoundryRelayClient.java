package com.campaignorganizer.interchange.foundry.adapter.out.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.util.List;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Minimal client for the self-hosted Foundry relay's plain HTTP+JSON surface
 * (ADR-0115: a third-party generic bridge — {@code foundryvtt-rest-api-relay}
 * — in front of a live Foundry VTT session; this backend never talks
 * WebSocket directly). Not a Spring bean — each call builds its own instance,
 * since the relay base URL and API key are per-world, not fixed at startup
 * like the {@code ai} context's provider clients.
 */
final class FoundryRelayClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private final RestClient restClient;

    FoundryRelayClient(String relayBaseUrl, String apiKey) {
        this(RestClient.builder()
                .baseUrl(relayBaseUrl)
                .defaultHeader("x-api-key", apiKey)
                .requestFactory(new SimpleClientHttpRequestFactory() {{
                    setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
                    setReadTimeout((int) READ_TIMEOUT.toMillis());
                }})
                .build());
    }

    /** Test seam: lets unit tests bind a {@code MockRestServiceServer} to the builder. */
    FoundryRelayClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /** {@code clientId}s of every Foundry session currently connected under this API key
     * (requires the {@code clients:read} scope). Confirmed against the real relay's
     * published reference (foundryrestapi.com/docs/api/clients) — the response is a
     * list of client objects (worldTitle, systemId, isOnline, ...), not a flat list of
     * id strings; only {@code clientId} is used here. */
    List<String> listClients() {
        ClientsResponse response = restClient.get()
                .uri("/clients")
                .retrieve()
                .body(ClientsResponse.class);
        if (response == null || response.clients() == null) {
            return List.of();
        }
        return response.clients().stream().map(ClientInfo::clientId).toList();
    }

    private record ClientsResponse(List<ClientInfo> clients) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ClientInfo(String clientId) {
    }
}
