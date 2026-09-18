package com.campaignorganizer.worldbuilding.application.wiki.port.in;

import java.util.List;
import java.util.UUID;

/** DTOs for the auto-link scan/apply use cases (ADR-0116). */
public final class AutolinkDtos {

    private AutolinkDtos() {
    }

    /** Every candidate mention found inside one source article's body. */
    public record AutolinkCandidateGroup(UUID articleId, String articleTitle, List<AutolinkMatch> matches) {
    }

    public record AutolinkMatch(UUID targetArticleId, List<String> candidateNames, String matchedText,
                                int occurrenceIndex, String snippet) {
    }

    /** A reviewed decision to convert one occurrence, writing {@code chosenName}
     * (one of that match's {@code candidateNames}) as the link's target. */
    public record AutolinkSelection(UUID targetArticleId, int occurrenceIndex, String chosenName) {
    }
}
