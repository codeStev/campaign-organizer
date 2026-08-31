package com.campaignorganizer.characters.application.statblock.port.out;

import java.util.Set;
import java.util.UUID;

/** ACL into the tagging context: statblock ids carrying a given tag (ADR-0083). */
public interface StatblockTagLookupPort {

    Set<UUID> statblockIdsTaggedWith(UUID worldId, String tag);
}
