package com.campaignorganizer.ai.application.service;

import com.campaignorganizer.ai.application.port.in.SummarizeSessionNotesUseCase;
import com.campaignorganizer.ai.domain.DraftResult;
import com.campaignorganizer.ai.domain.SessionNotesToSummarize;
import org.springframework.stereotype.Service;

/**
 * Summarizes a GM's private session notes into a short digest via whichever
 * configured provider answers first (ADR-0082), reusing the ADR-0064/0065
 * provider-fallback infrastructure via {@link ProviderFallbackTextGenerator}.
 * On-demand only — the result is never persisted. No transaction boundary of
 * its own; this call writes nothing.
 */
@Service
public class SummarizeSessionNotesService implements SummarizeSessionNotesUseCase {

    private static final String SYSTEM_PROMPT = """
            You are a summarizing assistant for a tabletop RPG game master's \
            private session notes. Condense the notes the owner gives you into \
            a short, clear digest (a few sentences or a short bullet list) they \
            can skim before their next session - capture what happened, key \
            decisions, and loose threads, in the owner's own terms. Do not add \
            events, names, or details the notes don't already contain.
            This wiki is system-agnostic: never include rules-system-specific \
            content of any kind - no stat blocks, ability scores or modifiers, \
            skill/ability checks, dice notation, saving throws, spell slots, \
            character levels, or mechanics or terminology tied to D&D, \
            Pathfinder, or any other specific ruleset. Describe events and \
            outcomes only in narrative, mechanics-free prose.
            Write only the summary itself - no preamble, no "Here is a summary", \
            no closing remarks.""";

    private final ProviderFallbackTextGenerator generator;

    public SummarizeSessionNotesService(ProviderFallbackTextGenerator generator) {
        this.generator = generator;
    }

    @Override
    public DraftResult summarize(SummarizeSessionNotesCommand command) {
        SessionNotesToSummarize notes = new SessionNotesToSummarize(command.notes());
        return generator.generate(SYSTEM_PROMPT, notes.notes());
    }
}
