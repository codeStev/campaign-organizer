package com.campaignorganizer.ai.adapter.out.llm;

import com.campaignorganizer.ai.application.port.out.TextGenerationFailedException;
import com.campaignorganizer.ai.application.port.out.TextGenerationPort;
import com.campaignorganizer.ai.domain.DraftResult;
import com.campaignorganizer.config.AppProperties;
import org.springframework.stereotype.Component;

/** Fallback provider, tried when Groq is unconfigured or fails/rate-limits. */
@Component
public class OpenRouterTextGenerationAdapter implements TextGenerationPort {

    private static final String BASE_URL = "https://openrouter.ai/api/v1";
    private static final String DEFAULT_MODEL = "deepseek/deepseek-r1:free";

    private final ChatCompletionClient client;

    public OpenRouterTextGenerationAdapter(AppProperties props) {
        AppProperties.Ai ai = props.ai();
        this.client = (ai != null && ai.openRouterApiKey() != null && !ai.openRouterApiKey().isBlank())
                ? new ChatCompletionClient(BASE_URL, ai.openRouterApiKey())
                : null;
    }

    @Override
    public String providerId() {
        return "openrouter";
    }

    @Override
    public String defaultModel() {
        return DEFAULT_MODEL;
    }

    @Override
    public boolean configured() {
        return client != null;
    }

    @Override
    public DraftResult generate(String systemPrompt, String userPrompt, String model) {
        if (client == null) {
            throw new TextGenerationFailedException("OpenRouter not configured (OPENROUTER_API_KEY unset)");
        }
        return new DraftResult(client.complete(model, systemPrompt, userPrompt), providerId());
    }
}
