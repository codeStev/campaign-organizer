package com.campaignorganizer.ai.adapter.out.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.campaignorganizer.ai.application.port.out.TextGenerationFailedException;
import java.io.IOException;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Unit tests for the one place the AI feature actually touches the network:
 * request shape (URL, auth header, OpenAI-compatible body) and how every HTTP
 * outcome maps to {@link TextGenerationFailedException} — the fallback chain in
 * {@code DraftArticleTextService} can only be as sound as this mapping.
 */
class ChatCompletionClientTest {

    private static final String BASE_URL = "https://llm.example.com/v1";

    private MockRestServiceServer server;
    private ChatCompletionClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ChatCompletionClient(builder.defaultHeader("Authorization", "Bearer test-key").build());
    }

    @Test
    void sendsOpenAiShapedPostAndExtractsTheMessageText() {
        server.expect(requestTo(URI.create(BASE_URL + "/chat/completions")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("test-model"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andRespond(withSuccess(
                        """
                        {"choices":[{"message":{"role":"assistant","content":"A gruff dockmaster."}}]}
                        """,
                        MediaType.APPLICATION_JSON));

        String text = client.complete("test-model", "be brief", "write a dockmaster");

        assertThat(text).isEqualTo("A gruff dockmaster.");
        server.verify();
    }

    @Test
    void httpErrorStatus_becomesTextGenerationFailed() {
        server.expect(requestTo(URI.create(BASE_URL + "/chat/completions")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("{\"error\":\"bad key\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.complete("m", "s", "u"))
                .isInstanceOf(TextGenerationFailedException.class)
                .hasMessageContaining("401");
    }

    @Test
    void connectionFailure_becomesTextGenerationFailed() throws IOException {
        server.expect(requestTo(URI.create(BASE_URL + "/chat/completions")))
                .andRespond(withException(new IOException("no route to host")));

        assertThatThrownBy(() -> client.complete("m", "s", "u"))
                .isInstanceOf(TextGenerationFailedException.class)
                .hasMessageContaining("no route to host");
    }

    @Test
    void emptyChoices_becomesTextGenerationFailed() {
        server.expect(requestTo(URI.create(BASE_URL + "/chat/completions")))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.complete("m", "s", "u"))
                .isInstanceOf(TextGenerationFailedException.class)
                .hasMessageContaining("Empty response");
    }

    @Test
    void blankContent_becomesTextGenerationFailed() {
        server.expect(requestTo(URI.create(BASE_URL + "/chat/completions")))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"  \"}}]}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.complete("m", "s", "u"))
                .isInstanceOf(TextGenerationFailedException.class)
                .hasMessageContaining("Empty response");
    }
}
