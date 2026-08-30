package com.campaignorganizer.handouts.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HandoutJpaRepository extends JpaRepository<HandoutJpaEntity, UUID> {

    /** Manually-ordered handouts first (FR-46 follow-up), then untouched ones by recency. */
    @Query("""
            SELECT h FROM HandoutJpaEntity h
            WHERE h.worldId = :worldId
            ORDER BY h.sortOrder ASC NULLS LAST, h.createdAt DESC
            """)
    List<HandoutJpaEntity> findByWorldOrdered(@Param("worldId") UUID worldId);

    Optional<HandoutJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);

    List<HandoutJpaEntity> findBySessionIdOrderByCreatedAtDesc(UUID sessionId);
}
