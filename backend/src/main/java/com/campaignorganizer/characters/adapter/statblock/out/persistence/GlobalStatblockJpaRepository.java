package com.campaignorganizer.characters.adapter.statblock.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface GlobalStatblockJpaRepository extends JpaRepository<GlobalStatblockJpaEntity, UUID> {

    List<GlobalStatblockJpaEntity> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    List<GlobalStatblockJpaEntity> findBySystemIdOrderByCreatedAtDesc(UUID systemId);

    Optional<GlobalStatblockJpaEntity> findBySystemIdAndName(UUID systemId, String name);

    boolean existsByGlobalTemplateId(UUID globalTemplateId);

    // flushAutomatically: the owning account's own INSERT (in the same transaction,
    // via AccountRepositoryPort.save) must be flushed before this bulk UPDATE runs,
    // or the FK reference doesn't exist in the database yet and this fails.
    @Modifying(flushAutomatically = true)
    @Query("update GlobalStatblockJpaEntity s set s.ownerId = :ownerId where s.ownerId is null")
    void assignUnownedTo(UUID ownerId);
}
