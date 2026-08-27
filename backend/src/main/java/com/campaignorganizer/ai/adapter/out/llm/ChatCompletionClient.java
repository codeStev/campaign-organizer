package com.campaignorganizer.ai.adapter.out.llm;

import com.campaignorganizer.ai.application.port.out.TextGenerationFailedException;
import java.time.Duration;
import java.util.List;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Minimal client for the OpenAI-compatible {@code /chat/completions} shape both
 * Groq and OpenRouter speak. Not a Spring bean — each provider adapter owns one
 * instance, since base URL/key/model differ per provider.
 */
final class ChatCompletionClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    private final RestClient restClient;

    ChatCompletionClient(String baseUrl, String apiKey) {
        this(RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .requestFactory(new SimpleClientHttpRequestFactory() {{
                    setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
                    setReadTimeout((int) READ_TIMEOUT.toMillis());
                }})
                .build());
    }

    /** Test seam: lets unit tests bind a {@code MockRestServiceServer} to the builder. */
    ChatCompletionClient(RestClient restClient) {
        this.restClient = restClient;
    }

    String complete(String model, String systemPrompt, String userPrompt) {
        ChatResponse response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .body(new ChatRequest(model, List.of(
                            new ChatMessage("system", systemPrompt),
                            new ChatMessage("user", userPrompt))))
                    .retrieve()
                    .body(ChatResponse.class);
        } catch (RestClientException e) {
            throw new TextGenerationFailedException(
                    "Chat completion call failed: " + e.getMessage(), e);
        }
        String text = extractText(response);
        if (text == null || text.isBlank()) {
            throw new TextGenerationFailedException("Empty response from provider");
        }
        return text;
    }

    private static String extractText(ChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return null;
        }
        ChatMessage message = response.choices().get(0).message();
        return message == null ? null : message.content();
    }

    private record ChatRequest(String model, List<ChatMessage> messages) {
    }

    private record ChatMessage(String role, String content) {
    }

    private record ChatResponse(List<Choice> choices) {
    }

    private record Choice(ChatMessage message) {
    }
}
