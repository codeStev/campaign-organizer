package com.campaignorganizer.campaign.application.arc.port.published;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Published port: read beats from sibling aggregates and other contexts (usage, packet, export, statblock). */
public interface ArcBeatQueryPort {

    List<ArcBeatView> findByArc(UUID arcId);

    List<ArcBeatView> findBySession(UUID sessionId);

    /** Beats that link the given article. */
    List<ArcBeatView> findByLinkedArticle(UUID articleId);

    /** Distinct article ids linked by any beat in the given arcs. */
    List<UUID> linkedArticleIdsByArcs(Collection<UUID> arcIds);

    /** Distinct statblock ids linked by any beat in the given arcs (ADR-0043). */
    List<UUID> linkedStatblockIdsByArcs(Collection<UUID> arcIds);
}
