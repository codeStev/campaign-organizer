package com.campaignorganizer.ai.adapter.out.llm;

import com.campaignorganizer.ai.application.port.out.TextGenerationFailedException;
import com.campaignorganizer.ai.application.port.out.TextGenerationPort;
import com.campaignorganizer.ai.domain.DraftResult;
import com.campaignorganizer.config.AppProperties;
import org.springframework.stereotype.Component;

/** Primary provider: fast, free tier with no card and no training on prompts. */
@Component
public class GroqTextGenerationAdapter implements TextGenerationPort {

    private static final String BASE_URL = "https://api.groq.com/openai/v1";
    private static final String DEFAULT_MODEL = "llama-3.3-70b-versatile";

    private final ChatCompletionClient client;

    public GroqTextGenerationAdapter(AppProperties props) {
        AppProperties.Ai ai = props.ai();
        this.client = (ai != null && ai.groqApiKey() != null && !ai.groqApiKey().isBlank())
                ? new ChatCompletionClient(BASE_URL, ai.groqApiKey())
                : null;
    }

    @Override
    public String providerId() {
        return "groq";
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
            throw new TextGenerationFailedException("Groq not configured (GROQ_API_KEY unset)");
        }
        return new DraftResult(client.complete(model, systemPrompt, userPrompt), providerId());
    }
}
