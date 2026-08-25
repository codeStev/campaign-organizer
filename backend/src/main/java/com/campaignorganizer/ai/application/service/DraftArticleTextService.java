package com.campaignorganizer.ai.application.service;

import com.campaignorganizer.ai.application.port.in.DraftArticleTextUseCase;
import com.campaignorganizer.ai.application.port.out.TextGenerationFailedException;
import com.campaignorganizer.ai.application.port.out.TextGenerationPort;
import com.campaignorganizer.ai.domain.AiUnavailableException;
import com.campaignorganizer.ai.domain.DraftInstructions;
import com.campaignorganizer.ai.domain.DraftResult;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Drafts article text via whichever configured provider answers first (ADR-0064).
 * No persistence, no transaction boundary — this context reads and writes nothing
 * of its own; the caller decides what to do with the draft.
 */
@Service
public class DraftArticleTextService implements DraftArticleTextUseCase {

    private static final String SYSTEM_PROMPT = """
            You are a drafting assistant for a tabletop RPG worldbuilding wiki. \
            Write a concise first draft in Markdown for the owner's own article, \
            in a tone consistent with any existing content they show you. \
            Write only the drafted text itself - no preamble, no "Here is a draft", \
            no closing remarks.""";

    private final List<TextGenerationPort> providers;

    public DraftArticleTextService(List<TextGenerationPort> providers) {
        this.providers = providers;
    }

    @Override
    public DraftResult draft(DraftArticleTextCommand command) {
        DraftInstructions instructions = new DraftInstructions(command.instructions(), command.existingContent());
        String userPrompt = toPrompt(instructions);
        for (TextGenerationPort provider : providers) {
            try {
                return provider.generate(SYSTEM_PROMPT, userPrompt);
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
