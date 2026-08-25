package com.campaignorganizer.ai.application.port.out;

import com.campaignorganizer.ai.domain.DraftResult;

/**
 * One text-generation provider. {@code DraftArticleTextService} resolves
 * try-order and model choice from persisted settings (ADR-0065) and calls
 * whichever ports it needs, in that order - this port itself expresses
 * neither fallback nor a fixed model.
 */
public interface TextGenerationPort {

    /** Stable id matching {@code ProviderSetting#providerId()}, e.g. {@code "groq"}. */
    String providerId();

    /** Used when no persisted setting (or a null model) says otherwise. */
    String defaultModel();

    /** Whether this provider has what it needs (e.g. an API key) to actually be called. */
    boolean configured();

    /**
     * @param model the model to use for this call (never null - the caller
     *     resolves {@link #defaultModel()} beforehand if needed).
     * @throws TextGenerationFailedException if this provider isn't configured or
     *     the call fails (network, timeout, non-2xx, empty response) — the caller
     *     is expected to try the next provider, not treat this as fatal.
     */
    DraftResult generate(String systemPrompt, String userPrompt, String model);
}
