package com.campaignorganizer.campaign.adapter.session.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionJpaRepository extends JpaRepository<SessionJpaEntity, UUID> {

    /** Ordered by session number then date; nulls last so un-numbered sessions trail. */
    @Query("""
            SELECT s FROM SessionJpaEntity s
            WHERE s.campaignId = :campaignId
            ORDER BY s.sessionNumber ASC NULLS LAST, s.date ASC NULLS LAST, s.createdAt ASC
            """)
    List<SessionJpaEntity> findOrdered(@Param("campaignId") UUID campaignId);

    Optional<SessionJpaEntity> findByIdAndCampaignId(UUID id, UUID campaignId);
}
