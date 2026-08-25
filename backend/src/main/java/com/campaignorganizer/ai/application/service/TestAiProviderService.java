package com.campaignorganizer.ai.application.service;

import com.campaignorganizer.ai.application.port.in.TestAiProviderUseCase;
import com.campaignorganizer.ai.application.port.out.AiProviderSettingsRepositoryPort;
import com.campaignorganizer.ai.application.port.out.TextGenerationFailedException;
import com.campaignorganizer.ai.application.port.out.TextGenerationPort;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * One trivial round-trip against a single provider (Settings "Test" button).
 * Mirrors {@link DraftArticleTextService}'s provider lookup but targets one
 * provider and never falls back — the point is to learn about *this* one.
 */
@Service
public class TestAiProviderService implements TestAiProviderUseCase {

    private static final String SYSTEM_PROMPT = "You are a connectivity check. Reply with the single word: OK.";
    private static final String USER_PROMPT = "Ping.";

    private final List<TextGenerationPort> providers;
    private final AiProviderSettingsRepositoryPort settings;

    public TestAiProviderService(List<TextGenerationPort> providers, AiProviderSettingsRepositoryPort settings) {
        this.providers = providers;
        this.settings = settings;
    }

    @Override
    public ProviderTestView test(String providerId) {
        // Resolved per call: a constructor must not invoke methods on its ports.
        TextGenerationPort provider = providers.stream()
                .filter(p -> p.providerId().equals(providerId))
                .findAny()
                .orElseThrow(() -> new NotFoundException("Unknown AI provider: " + providerId));
        if (!provider.configured()) {
            return new ProviderTestView(false, provider.defaultModel(), 0,
                    "No API key configured for this provider (GROQ_API_KEY / OPENROUTER_API_KEY)");
        }
        String model = persistedModel(providerId) != null ? persistedModel(providerId) : provider.defaultModel();
        long start = System.nanoTime();
        try {
            provider.generate(SYSTEM_PROMPT, USER_PROMPT, model);
        } catch (TextGenerationFailedException e) {
            return new ProviderTestView(false, model, elapsedMs(start), e.getMessage());
        }
        return new ProviderTestView(true, model, elapsedMs(start), null);
    }

    private String persistedModel(String providerId) {
        return settings.findAllOrderedByPriority().stream()
                .filter(s -> s.providerId().equals(providerId))
                .findAny()
                .map(s -> s.model())
                .orElse(null);
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
