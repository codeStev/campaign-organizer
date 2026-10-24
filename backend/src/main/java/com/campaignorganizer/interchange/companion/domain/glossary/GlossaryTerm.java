package com.campaignorganizer.interchange.companion.domain.glossary;

import java.util.List;

/** A proper noun with its alternate spellings; never carries any descriptive text (ADR-0120). */
public record GlossaryTerm(String name, GlossaryTermKind kind, List<String> aliases) {
}
