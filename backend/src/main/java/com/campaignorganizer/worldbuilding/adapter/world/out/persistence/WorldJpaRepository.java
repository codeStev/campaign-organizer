package com.campaignorganizer.worldbuilding.adapter.world.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface WorldJpaRepository extends JpaRepository<WorldJpaEntity, UUID> {

    List<WorldJpaEntity> findAllByOrderByCreatedAtDesc();

    List<WorldJpaEntity> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    // flushAutomatically: the owning account's own INSERT (in the same transaction,
    // via AccountRepositoryPort.save) must be flushed before this bulk UPDATE runs,
    // or the FK reference doesn't exist in the database yet and this fails.
    @Modifying(flushAutomatically = true)
    @Query("update WorldJpaEntity w set w.ownerId = :ownerId where w.ownerId is null")
    void assignUnownedTo(UUID ownerId);
}
