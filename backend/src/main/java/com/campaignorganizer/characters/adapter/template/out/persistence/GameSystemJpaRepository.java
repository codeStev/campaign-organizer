package com.campaignorganizer.characters.adapter.template.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface GameSystemJpaRepository extends JpaRepository<GameSystemJpaEntity, UUID> {

    List<GameSystemJpaEntity> findAllByOrderByNameAsc();

    List<GameSystemJpaEntity> findAllByOwnerIdOrderByNameAsc(UUID ownerId);

    Optional<GameSystemJpaEntity> findByOwnerIdAndNameIgnoreCase(UUID ownerId, String name);

    // flushAutomatically: the owning account's own INSERT (in the same transaction,
    // via AccountRepositoryPort.save) must be flushed before this bulk UPDATE runs,
    // or the FK reference doesn't exist in the database yet and this fails.
    @Modifying(flushAutomatically = true)
    @Query("update GameSystemJpaEntity g set g.ownerId = :ownerId where g.ownerId is null")
    void assignUnownedTo(UUID ownerId);
}
