package com.campaignorganizer.campaign.adapter.session.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionAttendanceJpaRepository extends JpaRepository<SessionAttendanceJpaEntity, UUID> {

    List<SessionAttendanceJpaEntity> findBySessionId(UUID sessionId);

    /**
     * Bulk delete, executed immediately rather than deferred to flush time:
     * a derived (non-{@code @Modifying}) delete only schedules entity removal
     * in the persistence context, and Hibernate's flush always runs pending
     * inserts before pending deletes regardless of scheduling order — so a
     * deferred delete here would let the replacement rows collide with the
     * not-yet-physically-deleted old ones on the unique constraint.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM SessionAttendanceJpaEntity a WHERE a.sessionId = :sessionId")
    void deleteBySessionId(@Param("sessionId") UUID sessionId);
}
