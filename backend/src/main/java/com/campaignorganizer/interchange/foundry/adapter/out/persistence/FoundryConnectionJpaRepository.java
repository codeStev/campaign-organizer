package com.campaignorganizer.interchange.foundry.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoundryConnectionJpaRepository extends JpaRepository<FoundryConnectionJpaEntity, UUID> {
}
