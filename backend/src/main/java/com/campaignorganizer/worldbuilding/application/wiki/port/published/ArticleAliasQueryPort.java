package com.campaignorganizer.worldbuilding.application.wiki.port.published;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Published port: article aliases (ADR-0116) for other contexts, e.g. the companion glossary (ADR-0120). */
public interface ArticleAliasQueryPort {

    /** Aliases per article id; articles without aliases are absent from the map. */
    Map<UUID, List<String>> findByArticlesInWorld(UUID worldId, Collection<UUID> articleIds);
}
