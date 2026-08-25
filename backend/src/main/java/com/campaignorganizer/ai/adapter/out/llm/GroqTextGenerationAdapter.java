package com.campaignorganizer.ai.adapter.out.llm;

import com.campaignorganizer.ai.application.port.out.TextGenerationFailedException;
import com.campaignorganizer.ai.application.port.out.TextGenerationPort;
import com.campaignorganizer.ai.domain.DraftResult;
import com.campaignorganizer.config.AppProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Primary provider: fast, free tier with no card and no training on prompts. */
@Component
@Order(1)
public class GroqTextGenerationAdapter implements TextGenerationPort {

    private static final String BASE_URL = "https://api.groq.com/openai/v1";

    private final ChatCompletionClient client;

    public GroqTextGenerationAdapter(AppProperties props) {
        AppProperties.Ai ai = props.ai();
        this.client = (ai != null && ai.groqApiKey() != null && !ai.groqApiKey().isBlank())
                ? new ChatCompletionClient(BASE_URL, ai.groqApiKey(), ai.groqModel())
                : null;
    }

    @Override
    public DraftResult generate(String systemPrompt, String userPrompt) {
        if (client == null) {
            throw new TextGenerationFailedException("Groq not configured (GROQ_API_KEY unset)");
        }
        return new DraftResult(client.complete(systemPrompt, userPrompt), "groq");
    }
}
