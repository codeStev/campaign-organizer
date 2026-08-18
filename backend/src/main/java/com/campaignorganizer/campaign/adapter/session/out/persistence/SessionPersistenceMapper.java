package com.campaignorganizer.campaign.adapter.session.out.persistence;

import com.campaignorganizer.campaign.domain.session.Session;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SessionPersistenceMapper {

    SessionJpaEntity toEntity(Session session);

    default Session toDomain(SessionJpaEntity e) {
        if (e == null) {
            return null;
        }
        return Session.reconstitute(e.getId(), e.getCampaignId(), e.getTitle(), e.getSessionNumber(),
                e.getDate(), e.getSummary(), e.getNotes(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
