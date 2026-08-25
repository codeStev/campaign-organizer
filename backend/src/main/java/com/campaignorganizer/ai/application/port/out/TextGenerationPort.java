package com.campaignorganizer.ai.application.port.out;

import com.campaignorganizer.ai.domain.DraftResult;

/**
 * One text-generation provider. Implementations are tried in order (see
 * {@code DraftArticleTextService}) — this port intentionally does not itself
 * express fallback; that's an application-layer policy, not adapter behavior.
 */
public interface TextGenerationPort {

    /**
     * @throws TextGenerationFailedException if this provider isn't configured or
     *     the call fails (network, timeout, non-2xx, empty response) — the caller
     *     is expected to try the next provider, not treat this as fatal.
     */
    DraftResult generate(String systemPrompt, String userPrompt);
}
