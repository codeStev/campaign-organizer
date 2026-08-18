package com.campaignorganizer.campaign.application.arc.service;

import com.campaignorganizer.campaign.application.arc.port.out.ArcBeatRepositoryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatView;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only beat queries (the published port). Kept free of write-side
 * dependencies (statblock/article/session existence) so it can be consumed by
 * the statblock context's campaign-ref ACL without a bean cycle.
 */
@Service
public class ArcBeatQueryService implements ArcBeatQueryPort {

    private final ArcBeatRepositoryPort beats;
    private final ArcBeatViewMapper viewMapper;

    public ArcBeatQueryService(ArcBeatRepositoryPort beats, ArcBeatViewMapper viewMapper) {
        this.beats = beats;
        this.viewMapper = viewMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArcBeatView> findByArc(UUID arcId) {
        return beats.findByArc(arcId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArcBeatView> findBySession(UUID sessionId) {
        return beats.findBySession(sessionId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArcBeatView> findByLinkedArticle(UUID articleId) {
        return beats.findByLinkedArticle(articleId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> linkedArticleIdsByArcs(Collection<UUID> arcIds) {
        return arcIds.isEmpty() ? List.of() : beats.linkedArticleIdsByArcs(arcIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> linkedStatblockIdsByArcs(Collection<UUID> arcIds) {
        return arcIds.isEmpty() ? List.of() : beats.linkedStatblockIdsByArcs(arcIds);
    }
}
