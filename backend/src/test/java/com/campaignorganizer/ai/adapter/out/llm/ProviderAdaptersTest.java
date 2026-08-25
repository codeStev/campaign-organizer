package com.campaignorganizer.ai.adapter.out.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.ai.application.port.out.TextGenerationFailedException;
import com.campaignorganizer.config.AppProperties;
import org.junit.jupiter.api.Test;

/**
 * Provider adapters are thin, but their configuration gate is what keeps a
 * missing key from ever becoming an HTTP call (AppProperties.Ai documents
 * "an unconfigured provider is skipped") — that gate is worth pinning down.
 */
class ProviderAdaptersTest {

    private static AppProperties props(String groqKey, String openRouterKey) {
        return new AppProperties("pw", null, null,
                new AppProperties.Ai(groqKey, openRouterKey, null, null));
    }

    @Test
    void blankKeys_leaveBothAdaptersUnconfigured() {
        var groq = new GroqTextGenerationAdapter(props("", "  "));
        var openRouter = new OpenRouterTextGenerationAdapter(props(null, ""));

        assertThat(groq.configured()).isFalse();
        assertThat(openRouter.configured()).isFalse();
    }

    @Test
    void presentKeys_configureBothAdapters() {
        var groq = new GroqTextGenerationAdapter(props("gsk_key", ""));
        var openRouter = new OpenRouterTextGenerationAdapter(props("", "sk-or-key"));

        assertThat(groq.configured()).isTrue();
        assertThat(openRouter.configured()).isTrue();
    }

    @Test
    void unconfiguredProvider_failsFastWithoutNetworkCall() {
        var groq = new GroqTextGenerationAdapter(props(null, "other"));

        assertThatThrownBy(() -> groq.generate("system", "user", "model"))
                .isInstanceOf(TextGenerationFailedException.class)
                .hasMessageContaining("GROQ_API_KEY");
    }

    @Test
    void providerIdsMatchThePersistedSettingsKeys() {
        assertThat(new GroqTextGenerationAdapter(props(null, null)).providerId()).isEqualTo("groq");
        assertThat(new OpenRouterTextGenerationAdapter(props(null, null)).providerId())
                .isEqualTo("openrouter");
    }
}
