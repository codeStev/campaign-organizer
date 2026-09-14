package com.campaignorganizer.characters.adapter.template.out.persistence;

import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface GlobalFieldTemplateJpaRepository extends JpaRepository<GlobalFieldTemplateJpaEntity, UUID> {

    List<GlobalFieldTemplateJpaEntity> findByOwnerIdAndKindOrderByCreatedAtDesc(UUID ownerId, TemplateKind kind);

    List<GlobalFieldTemplateJpaEntity> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    Optional<GlobalFieldTemplateJpaEntity> findByKindAndSystemIdAndName(TemplateKind kind, UUID systemId,
                                                                        String name);

    boolean existsBySystemId(UUID systemId);

    // flushAutomatically: the owning account's own INSERT (in the same transaction,
    // via AccountRepositoryPort.save) must be flushed before this bulk UPDATE runs,
    // or the FK reference doesn't exist in the database yet and this fails.
    @Modifying(flushAutomatically = true)
    @Query("update GlobalFieldTemplateJpaEntity t set t.ownerId = :ownerId where t.ownerId is null")
    void assignUnownedTo(UUID ownerId);
}
