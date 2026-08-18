package com.campaignorganizer.campaign.adapter.arc.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArcBeatJpaRepository extends JpaRepository<ArcBeatJpaEntity, UUID> {

    List<ArcBeatJpaEntity> findByArcIdOrderByPositionAscCreatedAtAsc(UUID arcId);

    Optional<ArcBeatJpaEntity> findByIdAndArcId(UUID id, UUID arcId);

    /** Beats scheduled into a session, in play order. */
    List<ArcBeatJpaEntity> findBySessionIdOrderByPositionAscCreatedAtAsc(UUID sessionId);

    /** Beats that link the given article (via the beat_articles collection). */
    @Query("SELECT b FROM ArcBeatJpaEntity b JOIN b.articleIds a WHERE a = :articleId")
    List<ArcBeatJpaEntity> findByLinkedArticleId(@Param("articleId") UUID articleId);

    /** Distinct article ids linked by any beat in the given arcs. */
    @Query("SELECT DISTINCT a FROM ArcBeatJpaEntity b JOIN b.articleIds a WHERE b.arcId IN :arcIds")
    List<UUID> findLinkedArticleIdsByArcIds(@Param("arcIds") Collection<UUID> arcIds);

    /** Distinct statblock ids linked by any beat in the given arcs (ADR-0043). */
    @Query("SELECT DISTINCT s FROM ArcBeatJpaEntity b JOIN b.statblockIds s WHERE b.arcId IN :arcIds")
    List<UUID> findLinkedStatblockIdsByArcIds(@Param("arcIds") Collection<UUID> arcIds);
}
