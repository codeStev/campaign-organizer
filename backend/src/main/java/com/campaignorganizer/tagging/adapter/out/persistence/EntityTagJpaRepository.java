package com.campaignorganizer.tagging.adapter.out.persistence;

import com.campaignorganizer.tagging.domain.EntityType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EntityTagJpaRepository extends JpaRepository<EntityTagJpaEntity, UUID> {

    List<EntityTagJpaEntity> findByWorldId(UUID worldId);

    List<EntityTagJpaEntity> findByWorldIdAndEntityTypeAndEntityId(UUID worldId,
            EntityType entityType, UUID entityId);

    /**
     * Bulk delete, not a derived {@code deleteBy...} method: a derived delete
     * only queues per-entity removals in the persistence context, and
     * Hibernate's flush always executes pending inserts before pending
     * deletes regardless of call order. {@link com.campaignorganizer.tagging
     * .application.service.TaggingService#set} deletes-then-reinserts the
     * whole tag set in one transaction, so any tag name kept across an edit
     * would have its reinsert flushed before its own delete, violating
     * {@code uq_entity_tags_entity_name}. A {@code @Modifying} bulk query
     * executes immediately against the database instead, so the delete is
     * really done before {@code save()} is called for the new rows.
     */
    @Modifying
    @Query("""
            DELETE FROM EntityTagJpaEntity t
            WHERE t.worldId = :worldId AND t.entityType = :entityType AND t.entityId = :entityId
            """)
    void deleteByWorldIdAndEntityTypeAndEntityId(@Param("worldId") UUID worldId,
            @Param("entityType") EntityType entityType, @Param("entityId") UUID entityId);

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
