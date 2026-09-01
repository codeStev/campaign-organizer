package com.campaignorganizer.worldbuilding.adapter.wiki.out.context;

import com.campaignorganizer.tagging.application.port.published.TagQueryPort;
import com.campaignorganizer.tagging.domain.EntityType;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleTagLookupPort;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: article ids carrying a tag, via the tagging context's published query port (ADR-0083). */
@Component
public class WikiArticleTagLookupAdapter implements ArticleTagLookupPort {

    private final TagQueryPort tags;

    public WikiArticleTagLookupAdapter(TagQueryPort tags) {
        this.tags = tags;
    }

    @Override
    public Set<UUID> articleIdsTaggedWith(UUID worldId, String tag) {
        return Set.copyOf(tags.entityIdsTaggedWith(worldId, EntityType.ARTICLE, tag));
    }

    @Override
    public Set<UUID> articleIdsTaggedContaining(UUID worldId, String fragment) {
        return Set.copyOf(tags.entityIdsWhereTagContains(worldId, EntityType.ARTICLE, fragment));
    }
}
