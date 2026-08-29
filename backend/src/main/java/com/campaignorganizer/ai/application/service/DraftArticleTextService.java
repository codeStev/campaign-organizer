package com.campaignorganizer.ai.application.service;

import com.campaignorganizer.ai.application.port.in.DraftArticleTextUseCase;
import com.campaignorganizer.ai.application.port.out.AiProviderSettingsRepositoryPort;
import com.campaignorganizer.ai.application.port.out.TextGenerationFailedException;
import com.campaignorganizer.ai.application.port.out.TextGenerationPort;
import com.campaignorganizer.ai.domain.AiUnavailableException;
import com.campaignorganizer.ai.domain.ArticleKind;
import com.campaignorganizer.ai.domain.DraftInstructions;
import com.campaignorganizer.ai.domain.DraftLevel;
import com.campaignorganizer.ai.domain.DraftResult;
import com.campaignorganizer.ai.domain.ProviderSetting;
import java.util.List;
import java.util.Map;
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

    private static final String INTRO_PREFIX =
            "You are a drafting assistant for a tabletop RPG worldbuilding wiki. ";

    private static final Map<DraftLevel, String> LEVEL_INTROS = Map.of(
            DraftLevel.QUICK_INSPIRATION, """
                    The owner wants a spark, not a finished article: write one \
                    short, evocative teaser (2-4 sentences, no headings, no \
                    bullet points) that captures a mood, hook, or image to get \
                    their imagination going - not a description, not a summary. \
                    Prefer suggestion over specification: it's fine to leave \
                    names, appearances, and backstory open for the owner to \
                    invent.""",
            DraftLevel.READ_ALOUD, """
                    Write a short passage of read-aloud/boxed text (3-6 \
                    sentences) meant to be spoken directly to the players at \
                    the table the moment they arrive somewhere or meet \
                    someone - second person ("you see/hear/smell..."), \
                    present tense, vivid sensory detail, no headings, no \
                    meta-commentary or GM-only asides, and nothing beyond \
                    what the players themselves would immediately perceive.""",
            DraftLevel.BASIC_INFO, """
                    Write only the essential factual information the owner's \
                    instructions (and any existing article content shown to \
                    you) actually call for - a few plain sentences or a short \
                    list, in Markdown. Do not invent or name any new places, \
                    characters, organizations, or other proper nouns beyond \
                    what the owner's instructions or existing content already \
                    establish; where a detail isn't given, leave it out or use \
                    a generic placeholder (e.g. "a nearby village") rather than \
                    making one up. This is a skeleton for the owner to flesh \
                    out, not a finished draft.""",
            DraftLevel.FULL_DRAFT, """
                    Write a concise first draft in Markdown for the owner's own \
                    article, in a tone consistent with any existing content \
                    they show you.""");

    private static final Map<ArticleKind, String> KIND_GUIDANCE = Map.ofEntries(
            Map.entry(ArticleKind.GENERIC, ""),
            Map.entry(ArticleKind.CHARACTER,
                    "This article is about a character: focus on personality, "
                            + "appearance, motivations, relationships, and their role in the story."),
            Map.entry(ArticleKind.LOCATION,
                    "This article is about a location: focus on atmosphere, "
                            + "notable features, and who or what can be found there."),
            Map.entry(ArticleKind.ORGANIZATION,
                    "This article is about an organization: focus on its "
                            + "purpose, structure, notable members, and influence."),
            Map.entry(ArticleKind.SPECIES,
                    "This article is about a species or people: focus on "
                            + "appearance, culture, and notable traits."),
            Map.entry(ArticleKind.ITEM,
                    "This article is about an item: focus on its appearance, "
                            + "history, and significance."),
            Map.entry(ArticleKind.EVENT,
                    "This article is about an event: focus on what happened, "
                            + "its causes, and its consequences."));

    private static final String SYSTEM_AGNOSTIC_CONSTRAINT = """
            This wiki is system-agnostic: never include rules-system-specific \
            content of any kind - no stat blocks, ability scores or modifiers, \
            skill/ability checks, dice notation, saving throws, spell slots, \
            character levels, or mechanics or terminology tied to D&D, \
            Pathfinder, or any other specific ruleset. Describe abilities, \
            items, and effects only in narrative, mechanics-free prose.""";

    private static final String OUTPUT_HYGIENE = """
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
        DraftInstructions instructions = new DraftInstructions(
                command.instructions(), command.existingContent(), command.level(), command.kind());
        String systemPrompt = buildSystemPrompt(instructions.level(), instructions.kind());
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

    private static String buildSystemPrompt(DraftLevel level, ArticleKind kind) {
        StringBuilder prompt = new StringBuilder(INTRO_PREFIX).append(LEVEL_INTROS.get(level));
        String kindClause = KIND_GUIDANCE.get(kind);
        if (!kindClause.isBlank()) {
            prompt.append(' ').append(kindClause);
        }
        return prompt.append('\n').append(SYSTEM_AGNOSTIC_CONSTRAINT).append('\n').append(OUTPUT_HYGIENE).toString();
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
