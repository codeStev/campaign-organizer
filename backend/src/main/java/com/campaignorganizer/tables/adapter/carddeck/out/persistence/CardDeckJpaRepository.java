package com.campaignorganizer.tables.adapter.carddeck.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardDeckJpaRepository extends JpaRepository<CardDeckJpaEntity, UUID> {

    List<CardDeckJpaEntity> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    Optional<CardDeckJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByIdAndWorldId(UUID id, UUID worldId);
}
