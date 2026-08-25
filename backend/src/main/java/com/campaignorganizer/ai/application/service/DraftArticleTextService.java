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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    private static final String SYSTEM_PROMPT = """
            You are a drafting assistant for a tabletop RPG worldbuilding wiki. \
            Write a concise first draft in Markdown for the owner's own article, \
            in a tone consistent with any existing content they show you. \
            Write only the drafted text itself - no preamble, no "Here is a draft", \
            no closing remarks.""";

    private final Map<String, TextGenerationPort> providersById;
    private final AiProviderSettingsRepositoryPort settings;

    public DraftArticleTextService(List<TextGenerationPort> providers, AiProviderSettingsRepositoryPort settings) {
        this.providersById = providers.stream()
                .collect(Collectors.toMap(TextGenerationPort::providerId, Function.identity()));
        this.settings = settings;
    }

    @Override
    public DraftResult draft(DraftArticleTextCommand command) {
        DraftInstructions instructions = new DraftInstructions(command.instructions(), command.existingContent());
        String userPrompt = toPrompt(instructions);
        List<ProviderSetting> order = DefaultProviderSettings.orDefaults(settings.findAllOrderedByPriority());
        for (ProviderSetting setting : order) {
            TextGenerationPort provider = providersById.get(setting.providerId());
            if (provider == null) {
                continue; // A setting for a provider that no longer exists in this build.
            }
            String model = setting.model() != null ? setting.model() : provider.defaultModel();
            try {
                return provider.generate(SYSTEM_PROMPT, userPrompt, model);
            } catch (TextGenerationFailedException ignored) {
                // Try the next configured provider.
            }
        }
        throw new AiUnavailableException(
                "No AI provider is configured or reachable. Set GROQ_API_KEY and/or OPENROUTER_API_KEY.");
    }

    private static String toPrompt(DraftInstructions instructions) {
        if (instructions.existingContent().isBlank()) {
            return "Instructions: " + instructions.instructions();
        }
        return "Existing article content so far:\n" + instructions.existingContent()
                + "\n\nInstructions for what to draft next: " + instructions.instructions();
    }
}
