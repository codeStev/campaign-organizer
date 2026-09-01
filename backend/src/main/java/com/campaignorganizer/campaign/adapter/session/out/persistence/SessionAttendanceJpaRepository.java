package com.campaignorganizer.campaign.adapter.session.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionAttendanceJpaRepository extends JpaRepository<SessionAttendanceJpaEntity, UUID> {

    List<SessionAttendanceJpaEntity> findBySessionId(UUID sessionId);

    void deleteBySessionId(UUID sessionId);
}
