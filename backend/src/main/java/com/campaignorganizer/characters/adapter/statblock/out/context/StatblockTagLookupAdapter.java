package com.campaignorganizer.characters.adapter.statblock.out.context;

import com.campaignorganizer.characters.application.statblock.port.out.StatblockTagLookupPort;
import com.campaignorganizer.tagging.application.port.published.TagQueryPort;
import com.campaignorganizer.tagging.domain.EntityType;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: statblock ids carrying a tag, via the tagging context's published query port (ADR-0083). */
@Component
public class StatblockTagLookupAdapter implements StatblockTagLookupPort {

    private final TagQueryPort tags;

    public StatblockTagLookupAdapter(TagQueryPort tags) {
        this.tags = tags;
    }

    @Override
    public Set<UUID> statblockIdsTaggedWith(UUID worldId, String tag) {
        return Set.copyOf(tags.entityIdsTaggedWith(worldId, EntityType.STATBLOCK, tag));
    }
}
