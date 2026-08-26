package com.campaignorganizer.campaign.adapter.session.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheatSheetJpaRepository extends JpaRepository<CheatSheetJpaEntity, UUID> {

    Optional<CheatSheetJpaEntity> findBySessionId(UUID sessionId);
}
