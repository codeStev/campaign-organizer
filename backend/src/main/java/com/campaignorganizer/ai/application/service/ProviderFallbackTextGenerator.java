package com.campaignorganizer.ai.application.service;

import com.campaignorganizer.ai.application.port.out.AiProviderSettingsRepositoryPort;
import com.campaignorganizer.ai.application.port.out.TextGenerationFailedException;
import com.campaignorganizer.ai.application.port.out.TextGenerationPort;
import com.campaignorganizer.ai.domain.AiUnavailableException;
import com.campaignorganizer.ai.domain.DraftResult;
import com.campaignorganizer.ai.domain.ProviderSetting;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Tries each configured provider in the order and with the model currently
 * persisted (ADR-0065) - a settings change takes effect on the very next
 * call, no restart - skipping unconfigured providers and falling back past
 * failures. Shared by every AI text-generation use case in this context
 * ({@link DraftArticleTextService}, {@link SummarizeSessionNotesService}):
 * they differ only in the prompts they build, not in how a provider is
 * chosen or how failure is handled.
 */
@Component
class ProviderFallbackTextGenerator {

    private static final Logger log = LoggerFactory.getLogger(ProviderFallbackTextGenerator.class);

    private final List<TextGenerationPort> providers;
    private final AiProviderSettingsRepositoryPort settings;

    ProviderFallbackTextGenerator(List<TextGenerationPort> providers, AiProviderSettingsRepositoryPort settings) {
        this.providers = providers;
        this.settings = settings;
    }

    DraftResult generate(String systemPrompt, String userPrompt) {
        List<ProviderSetting> order = DefaultProviderSettings.orDefaults(settings.findAllOrderedByPriority());
        for (ProviderSetting setting : order) {
            TextGenerationPort provider = byId(setting.providerId());
            if (provider == null) {
                continue; // A setting for a provider that no longer exists in this build.
            }
            if (!provider.configured()) {
                continue; // No API key for it — skipping is the documented behavior
                          // (AppProperties.Ai); attempting would just burn a doomed call.
            }
            String model = setting.model() != null ? setting.model() : provider.defaultModel();
            try {
                return provider.generate(systemPrompt, userPrompt, model);
            } catch (TextGenerationFailedException e) {
                // Try the next configured provider — but say why this one failed,
                // or "keys present yet unreachable" stays indistinguishable from
                // "keys missing" (the generic message below mentions only keys).
                log.warn("AI provider '{}' failed: {}", setting.providerId(), e.getMessage());
            }
        }
        throw new AiUnavailableException(
                "No AI provider succeeded — see the backend log for each provider's "
                        + "error. Check GROQ_API_KEY / OPENROUTER_API_KEY and outbound "
                        + "network access.");
    }

    /** Resolved per call: a constructor must not invoke methods on its ports. */
    private TextGenerationPort byId(String providerId) {
        return providers.stream()
                .filter(p -> providerId.equals(p.providerId()))
                .findAny()
                .orElse(null);
    }
}
