package com.campaignorganizer.interchange.foundry.adapter.out.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Minimal client for the self-hosted Foundry relay's plain HTTP+JSON surface
 * (ADR-0115: a third-party generic bridge — {@code foundryvtt-rest-api-relay}
 * — in front of a live Foundry VTT session; this backend never talks
 * WebSocket directly). Not a Spring bean — each call builds its own instance,
 * since the relay base URL, API key, and clientId are all per-world, not
 * fixed at startup like the {@code ai} context's provider clients. Every
 * request shape here is confirmed against the relay's own published
 * reference (foundryrestapi.com/docs/api), not assumed.
 */
final class FoundryRelayClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private final RestClient restClient;
    private final String clientId;

    FoundryRelayClient(String relayBaseUrl, String apiKey, String clientId) {
        this(RestClient.builder()
                .baseUrl(relayBaseUrl)
                .defaultHeader("x-api-key", apiKey)
                .requestFactory(new SimpleClientHttpRequestFactory() {{
                    setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
                    setReadTimeout((int) READ_TIMEOUT.toMillis());
                }})
                .build(), clientId);
    }

    /** Test seam: lets unit tests bind a {@code MockRestServiceServer} to the builder. */
    FoundryRelayClient(RestClient restClient, String clientId) {
        this.restClient = restClient;
        this.clientId = clientId;
    }

    /** {@code clientId}s of every Foundry session currently connected under this API key
     * (requires the {@code clients:read} scope; no {@code clientId} query param on this
     * particular endpoint — it's how you discover one). Confirmed against the real relay's
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

    /** Idempotent upsert of any Foundry document type (requires the {@code entity:write}
     * scope). {@code folder} may be {@code null} for documents that don't go in one. */
    void create(String entityType, Map<String, Object> data, String folder) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("entityType", entityType);
        body.put("data", data);
        if (folder != null) {
            body.put("folder", folder);
        }
        body.put("keepId", true);
        body.put("override", true);
        restClient.post()
                .uri(uriBuilder -> uriBuilder.path("/create").queryParam("clientId", clientId).build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    /** Uploads {@code bytes} to {@code path/filename} under Foundry's {@code source}
     * (always {@code "data"} for this feature) storage area, overwriting any existing
     * file at that exact path (requires the {@code file:write} scope). Returns the
     * relay-reported path, used verbatim. */
    String upload(String path, String source, String filename, String contentType, byte[] bytes) {
        String dataUri = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        Map<String, Object> body = Map.of(
                "fileData", dataUri,
                "mimeType", contentType,
                "overwrite", true);
        UploadResponse response = restClient.post()
                .uri(uriBuilder -> uriBuilder.path("/upload")
                        .queryParam("clientId", clientId)
                        .queryParam("path", path)
                        .queryParam("source", source)
                        .queryParam("filename", filename)
                        .queryParam("mimeType", contentType)
                        .queryParam("overwrite", true)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(UploadResponse.class);
        return response == null ? null : response.path();
    }

    private record ClientsResponse(List<ClientInfo> clients) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ClientInfo(String clientId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UploadResponse(String path) {
    }
}
