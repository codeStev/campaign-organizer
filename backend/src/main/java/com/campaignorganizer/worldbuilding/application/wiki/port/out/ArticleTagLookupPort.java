package com.campaignorganizer.worldbuilding.application.wiki.port.out;

import java.util.Set;
import java.util.UUID;

/** ACL into the tagging context: article ids carrying a given tag (ADR-0083). */
public interface ArticleTagLookupPort {

    Set<UUID> articleIdsTaggedWith(UUID worldId, String tag);
}
