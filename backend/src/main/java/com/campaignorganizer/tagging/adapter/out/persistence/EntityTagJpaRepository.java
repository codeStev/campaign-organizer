package com.campaignorganizer.tagging.adapter.out.persistence;

import com.campaignorganizer.tagging.domain.EntityType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EntityTagJpaRepository extends JpaRepository<EntityTagJpaEntity, UUID> {

    List<EntityTagJpaEntity> findByWorldId(UUID worldId);

    List<EntityTagJpaEntity> findByWorldIdAndEntityTypeAndEntityId(UUID worldId,
            EntityType entityType, UUID entityId);

    void deleteByWorldIdAndEntityTypeAndEntityId(UUID worldId, EntityType entityType,
            UUID entityId);

    @Query("""
            SELECT t.entityId FROM EntityTagJpaEntity t
            WHERE t.worldId = :worldId AND t.entityType = :entityType AND t.name = :name
            """)
    List<UUID> findEntityIdsByWorldIdAndEntityTypeAndName(@Param("worldId") UUID worldId,
            @Param("entityType") EntityType entityType, @Param("name") String name);

    @Query("SELECT DISTINCT t.name FROM EntityTagJpaEntity t WHERE t.worldId = :worldId")
    List<String> findDistinctNamesByWorldId(@Param("worldId") UUID worldId);

    @Query("""
            SELECT DISTINCT t.entityId FROM EntityTagJpaEntity t
            WHERE t.worldId = :worldId AND t.entityType = :entityType
              AND t.name LIKE CONCAT('%', :fragment, '%')
            """)
    List<UUID> findEntityIdsByWorldIdAndEntityTypeAndNameContaining(@Param("worldId") UUID worldId,
            @Param("entityType") EntityType entityType, @Param("fragment") String fragment);
}
