package com.campaignorganizer.campaign.adapter.session.out.persistence;

import com.campaignorganizer.campaign.application.session.port.out.SessionAttendanceRepositoryPort;
import com.campaignorganizer.campaign.domain.session.SessionAttendance;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** JPA-backed implementation of the session-attendance repository port. */
@Component
public class SessionAttendancePersistenceAdapter implements SessionAttendanceRepositoryPort {

    private final SessionAttendanceJpaRepository repository;
    private final SessionAttendancePersistenceMapper mapper;

    public SessionAttendancePersistenceAdapter(SessionAttendanceJpaRepository repository,
                                               SessionAttendancePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<SessionAttendance> findBySession(UUID sessionId) {
        return repository.findBySessionId(sessionId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public SessionAttendance save(SessionAttendance row) {
        return mapper.toDomain(repository.save(mapper.toEntity(row)));
    }

    @Override
    public void deleteBySession(UUID sessionId) {
        repository.deleteBySessionId(sessionId);
    }
}
