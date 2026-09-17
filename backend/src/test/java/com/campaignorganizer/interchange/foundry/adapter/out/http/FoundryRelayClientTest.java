package com.campaignorganizer.interchange.foundry.adapter.out.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Unit tests for the one place the Foundry push feature actually touches the
 * network. Every request/response shape asserted here is confirmed against
 * the relay's own published reference (foundryrestapi.com/docs/api), not
 * assumed — the {@code /clients} fixture in particular is copied verbatim
 * from the docs, since an earlier version of this client got that response
 * shape wrong (assumed {@code List<String>}, the real shape is a list of
 * client objects, and would have failed to deserialize against the real
 * relay).
 */
class FoundryRelayClientTest {

    private static final String BASE_URL = "https://relay.example.com";

    private MockRestServiceServer server;
    private FoundryRelayClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FoundryRelayClient(builder.defaultHeader("x-api-key", "test-key").build(), "test-client");
    }

    @Test
    void listClients_parsesClientIdOutOfTheRealResponseShape() {
        server.expect(requestToUriTemplate(BASE_URL + "/clients"))
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
        server.expect(requestToUriTemplate(BASE_URL + "/clients"))
                .andRespond(withSuccess("{\"clients\":[],\"total\":0}", MediaType.APPLICATION_JSON));

        assertThat(client.listClients()).isEmpty();
    }

    @Test
    void create_sendsClientIdAsQueryParamAndFolderAsTopLevelField() {
        server.expect(requestTo(startsWith(BASE_URL + "/create")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "test-key"))
                .andExpect(queryParam("clientId", "test-client"))
                .andExpect(jsonPath("$.entityType").value("JournalEntry"))
                .andExpect(jsonPath("$.data.name").value("My Article"))
                .andExpect(jsonPath("$.folder").value("folder123456789a"))
                .andExpect(jsonPath("$.keepId").value(true))
                .andExpect(jsonPath("$.override").value(true))
                // The most important shape assertion here: "folder" must be a top-level
                // field, not nested inside "data" — the relay's actual API disagrees with
                // this feature's own first (uncorrected) draft on exactly this point.
                .andExpect(jsonPath("$.data.folder").doesNotExist())
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        client.create("JournalEntry", Map.of("_id", "doc123456789abc", "name", "My Article"),
                "folder123456789a");

        server.verify();
    }

    @Test
    void create_rollTableEntityTypeCarriesFormulaAndResultsWithFolderStillTopLevel() {
        server.expect(requestTo(startsWith(BASE_URL + "/create")))
                .andExpect(jsonPath("$.entityType").value("RollTable"))
                .andExpect(jsonPath("$.data.formula").value("1d6"))
                .andExpect(jsonPath("$.data.results[0].range[0]").value(1))
                .andExpect(jsonPath("$.data.results[0].range[1]").value(3))
                .andExpect(jsonPath("$.data.results[0].description").value("<p>Goblin</p>"))
                .andExpect(jsonPath("$.folder").value("folder123456789a"))
                .andExpect(jsonPath("$.data.folder").doesNotExist())
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        Map<String, Object> result = Map.of(
                "_id", "result123456789a",
                "range", List.of(1, 3),
                "description", "<p>Goblin</p>",
                "type", "text");
        client.create("RollTable",
                Map.of("_id", "table123456789ab", "name", "A Table", "formula", "1d6", "results",
                        List.of(result)),
                "folder123456789a");

        server.verify();
    }

    @Test
    void create_cardsEntityTypeCarriesDeckTypeAndCardsWithFolderStillTopLevel() {
        server.expect(requestTo(startsWith(BASE_URL + "/create")))
                .andExpect(jsonPath("$.entityType").value("Cards"))
                .andExpect(jsonPath("$.data.type").value("deck"))
                .andExpect(jsonPath("$.data.cards[0].name").value("Ace"))
                .andExpect(jsonPath("$.data.cards[0].description").value("<p>Draw one</p>"))
                .andExpect(jsonPath("$.folder").value("folder123456789a"))
                .andExpect(jsonPath("$.data.folder").doesNotExist())
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        Map<String, Object> card = Map.of(
                "_id", "card1234567890ab",
                "name", "Ace",
                "description", "<p>Draw one</p>");
        client.create("Cards",
                Map.of("_id", "deck1234567890ab", "name", "A Deck", "type", "deck", "cards", List.of(card)),
                "folder123456789a");

        server.verify();
    }

    @Test
    void create_omitsFolderFieldWhenNull() {
        server.expect(requestTo(startsWith(BASE_URL + "/create")))
                .andExpect(jsonPath("$.folder").doesNotExist())
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        client.create("Folder", Map.of("_id", "folder123456789a", "name", "Articles"), null);

        server.verify();
    }

    @Test
    void upload_sendsBase64FileDataAndQueryParams() {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);

        server.expect(requestTo(startsWith(BASE_URL + "/upload")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(queryParam("clientId", "test-client"))
                .andExpect(queryParam("path", "campaign-organizer/world1"))
                .andExpect(queryParam("source", "data"))
                .andExpect(queryParam("filename", "img.png"))
                .andExpect(queryParam("mimeType", "image/png"))
                .andExpect(queryParam("overwrite", "true"))
                .andExpect(jsonPath("$.mimeType").value("image/png"))
                .andExpect(jsonPath("$.overwrite").value(true))
                .andExpect(jsonPath("$.fileData").value("data:image/png;base64,aGVsbG8="))
                .andRespond(withSuccess(
                        "{\"success\":true,\"path\":\"campaign-organizer/world1/img.png\"}",
                        MediaType.APPLICATION_JSON));

        String path = client.upload("campaign-organizer/world1", "data", "img.png", "image/png", bytes);

        assertThat(path).isEqualTo("campaign-organizer/world1/img.png");
        server.verify();
    }
}
