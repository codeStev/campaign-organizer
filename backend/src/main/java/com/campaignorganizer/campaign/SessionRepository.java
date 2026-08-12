package com.campaignorganizer.campaign;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    /** Ordered by session number then date; nulls last so un-numbered sessions trail. */
    @Query("""
            SELECT s FROM Session s
            WHERE s.campaignId = :campaignId
            ORDER BY s.sessionNumber ASC NULLS LAST, s.date ASC NULLS LAST, s.createdAt ASC
            """)
    List<Session> findOrdered(@Param("campaignId") UUID campaignId);

    Optional<Session> findByIdAndCampaignId(UUID id, UUID campaignId);
}
