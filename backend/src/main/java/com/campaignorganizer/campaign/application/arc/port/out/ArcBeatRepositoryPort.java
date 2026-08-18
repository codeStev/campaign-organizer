package com.campaignorganizer.campaign.application.arc.port.out;

import com.campaignorganizer.campaign.domain.arc.ArcBeat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArcBeatRepositoryPort {

    List<ArcBeat> findByArc(UUID arcId);

    Optional<ArcBeat> findByIdAndArc(UUID beatId, UUID arcId);

    List<ArcBeat> findBySession(UUID sessionId);

    List<ArcBeat> findByLinkedArticle(UUID articleId);

    List<UUID> linkedArticleIdsByArcs(Collection<UUID> arcIds);

    List<UUID> linkedStatblockIdsByArcs(Collection<UUID> arcIds);

    ArcBeat save(ArcBeat beat);

    void delete(ArcBeat beat);
}
