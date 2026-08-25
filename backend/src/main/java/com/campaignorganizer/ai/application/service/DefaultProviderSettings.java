package com.campaignorganizer.ai.application.service;

import com.campaignorganizer.ai.domain.ProviderSetting;
import java.util.ArrayList;
import java.util.List;

/**
 * The try-order used when nothing has been saved yet (ADR-0065) - shared by
 * {@code DraftArticleTextService} (needs the raw priority order to drive its
 * fallback loop) and {@code AiSettingsService} (needs the same fallback for
 * what it shows/returns before the owner has ever opened Settings). Not a
 * port: this is a small application-internal default, not a business rule
 * belonging in the domain model, and not something another context needs.
 */
final class DefaultProviderSettings {

    /** Matches ADR-0064's original choice: Groq primary, OpenRouter fallback. */
    private static final List<String> ORDER = List.of("groq", "openrouter");

    private DefaultProviderSettings() {
    }

    static List<ProviderSetting> orDefaults(List<ProviderSetting> saved) {
        if (!saved.isEmpty()) {
            return saved;
        }
        List<ProviderSetting> defaults = new ArrayList<>();
        int priority = 0;
        for (String providerId : ORDER) {
            defaults.add(new ProviderSetting(providerId, null, priority++));
        }
        return defaults;
    }
}
