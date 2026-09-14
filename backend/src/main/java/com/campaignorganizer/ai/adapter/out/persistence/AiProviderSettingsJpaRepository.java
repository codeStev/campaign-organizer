package com.campaignorganizer.ai.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AiProviderSettingsJpaRepository extends JpaRepository<AiProviderSettingsJpaEntity, UUID> {

    List<AiProviderSettingsJpaEntity> findAllByOwnerIdOrderByPriorityAsc(UUID ownerId);

    void deleteAllByOwnerId(UUID ownerId);

    // flushAutomatically: the owning account's own INSERT (in the same transaction,
    // via AccountRepositoryPort.save) must be flushed before this bulk UPDATE runs,
    // or the FK reference doesn't exist in the database yet and this fails.
    @Modifying(flushAutomatically = true)
    @Query("update AiProviderSettingsJpaEntity a set a.ownerId = :ownerId where a.ownerId is null")
    void assignUnownedTo(UUID ownerId);
}
