package com.campaignorganizer.interchange.foundry.adapter.out.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Unit tests for the one place the Foundry push feature actually touches the
 * network. The {@code /clients} fixture below is copied verbatim from the
 * relay's own published reference (foundryrestapi.com/docs/api/clients) — a
 * list of client *objects*, not a flat list of id strings, which an earlier
 * version of this client got wrong (assumed {@code List<String>} and would
 * have failed to deserialize against the real relay).
 */
class FoundryRelayClientTest {

    private static final String BASE_URL = "https://relay.example.com";

    private MockRestServiceServer server;
    private FoundryRelayClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FoundryRelayClient(builder.defaultHeader("x-api-key", "test-key").build());
    }

    @Test
    void listClients_parsesClientIdOutOfTheRealResponseShape() {
        server.expect(requestTo(URI.create(BASE_URL + "/clients")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("x-api-key", "test-key"))
                .andRespond(withSuccess(
                        """
                        {
                          "clients": [
                            {
                              "clientId": "qsl-integration-test",
                              "instanceId": "local",
                              "lastSeen": 1782956903871,
                              "connectedSince": 1782956903870,
                              "worldId": "test-world",
                              "worldTitle": "Test World",
                              "foundryVersion": "14.363",
                              "systemId": "dnd5e",
                              "systemTitle": "Dungeons & Dragons Fifth Edition",
                              "systemVersion": "5.0.4",
                              "publicUrl": "http://foundry:30000",
                              "ipAddress": "172.24.0.3:37168",
                              "tokenName": "headless session 2026-07-02 01:48",
                              "isOnline": true
                            }
                          ],
                          "total": 1
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        List<String> clientIds = client.listClients();

        assertThat(clientIds).containsExactly("qsl-integration-test");
        server.verify();
    }

    @Test
    void listClients_emptyListWhenNoneConnected() {
        server.expect(requestTo(URI.create(BASE_URL + "/clients")))
                .andRespond(withSuccess("{\"clients\":[],\"total\":0}", MediaType.APPLICATION_JSON));

        assertThat(client.listClients()).isEmpty();
    }
}
