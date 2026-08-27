package com.campaignorganizer.ai.application.service;

import com.campaignorganizer.ai.application.port.in.DraftArticleTextUseCase;
import com.campaignorganizer.ai.application.port.out.AiProviderSettingsRepositoryPort;
import com.campaignorganizer.ai.application.port.out.TextGenerationFailedException;
import com.campaignorganizer.ai.application.port.out.TextGenerationPort;
import com.campaignorganizer.ai.domain.AiUnavailableException;
import com.campaignorganizer.ai.domain.DraftInstructions;
import com.campaignorganizer.ai.domain.DraftResult;
import com.campaignorganizer.ai.domain.ProviderSetting;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Drafts article text via whichever configured provider answers first, in the
 * order and with the model currently persisted (ADR-0065) — a settings change
 * takes effect on the very next call, no restart. Reads settings through the
 * out-port directly (not {@code GetAiSettingsUseCase}, which returns the
 * richer display-oriented view the Settings UI needs) since this is one
 * application service reading its own context's state, not a use case in
 * itself. No transaction boundary of its own; this call writes nothing.
 */
@Service
public class DraftArticleTextService implements DraftArticleTextUseCase {

    private static final Logger log = LoggerFactory.getLogger(DraftArticleTextService.class);

    private static final String SYSTEM_PROMPT = """
            You are a drafting assistant for a tabletop RPG worldbuilding wiki. \
            Write a concise first draft in Markdown for the owner's own article, \
            in a tone consistent with any existing content they show you. \
            This wiki is system-agnostic: never include rules-system-specific \
            content of any kind - no stat blocks, ability scores or modifiers, \
            skill/ability checks, dice notation, saving throws, spell slots, \
            character levels, or mechanics or terminology tied to D&D, \
            Pathfinder, or any other specific ruleset. Describe abilities, \
            items, and effects only in narrative, mechanics-free prose. \
            Write only the drafted text itself - no preamble, no "Here is a draft", \
            no closing remarks.""";

    private final List<TextGenerationPort> providers;
    private final AiProviderSettingsRepositoryPort settings;

    public DraftArticleTextService(List<TextGenerationPort> providers, AiProviderSettingsRepositoryPort settings) {
        this.providers = providers;
        this.settings = settings;
    }

    @Override
    public DraftResult draft(DraftArticleTextCommand command) {
        DraftInstructions instructions = new DraftInstructions(command.instructions(), command.existingContent());
        String userPrompt = toPrompt(instructions);
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
                return provider.generate(SYSTEM_PROMPT, userPrompt, model);
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

    private static String toPrompt(DraftInstructions instructions) {
        if (instructions.existingContent().isBlank()) {
            return "Instructions: " + instructions.instructions();
        }
        return "Existing article content so far:\n" + instructions.existingContent()
                + "\n\nInstructions for what to draft next: " + instructions.instructions();
    }
}
