package com.campaignorganizer.campaign.adapter.session.out.persistence;

import com.campaignorganizer.campaign.domain.session.SessionAttendance;
import org.mapstruct.Mapper;

/** Maps the domain aggregate to/from its JPA entity (MapStruct). */
@Mapper(componentModel = "spring")
public interface SessionAttendancePersistenceMapper {

    SessionAttendanceJpaEntity toEntity(SessionAttendance row);

    /** The aggregate is immutable with a static factory, so reconstitute it explicitly. */
    default SessionAttendance toDomain(SessionAttendanceJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return SessionAttendance.reconstitute(entity.getId(), entity.getSessionId(),
                entity.getPlayerId(), entity.isPresent(), entity.getCharacterId(),
                entity.getCreatedAt());
    }
}
