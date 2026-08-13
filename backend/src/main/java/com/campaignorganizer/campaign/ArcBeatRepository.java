package com.campaignorganizer.campaign;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArcBeatRepository extends JpaRepository<ArcBeat, UUID> {

    List<ArcBeat> findByArcIdOrderByPositionAscCreatedAtAsc(UUID arcId);

    Optional<ArcBeat> findByIdAndArcId(UUID id, UUID arcId);

    /** Beats that link the given article (via the beat_articles collection). */
    @Query("SELECT b FROM ArcBeat b JOIN b.articleIds a WHERE a = :articleId")
    List<ArcBeat> findByLinkedArticleId(@Param("articleId") UUID articleId);

    /** Distinct article ids linked by any beat in the given arcs. */
    @Query("SELECT DISTINCT a FROM ArcBeat b JOIN b.articleIds a WHERE b.arcId IN :arcIds")
    List<UUID> findLinkedArticleIdsByArcIds(@Param("arcIds") Collection<UUID> arcIds);
}
