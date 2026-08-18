package com.campaignorganizer.worldbuilding.application.wiki.port.in;

import java.util.Set;
import java.util.UUID;

/**
 * Criteria for listing articles. Precedence: {@code restrictToIds} (campaign
 * filter, ADR-0033) &gt; {@code query} (fuzzy search, ADR-0022) &gt;
 * {@code categoryId} &gt; all. Null fields are ignored.
 */
public record ArticleListQuery(UUID worldId, UUID categoryId, String query, Set<UUID> restrictToIds) {
}
