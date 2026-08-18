package com.campaignorganizer.worldbuilding.adapter.world.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorldJpaRepository extends JpaRepository<WorldJpaEntity, UUID> {

    List<WorldJpaEntity> findAllByOrderByCreatedAtDesc();
}
