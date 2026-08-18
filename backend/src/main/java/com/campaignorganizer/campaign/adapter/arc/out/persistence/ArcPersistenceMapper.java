package com.campaignorganizer.campaign.adapter.arc.out.persistence;

import com.campaignorganizer.campaign.domain.arc.Arc;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArcPersistenceMapper {

    ArcJpaEntity toEntity(Arc arc);

    default Arc toDomain(ArcJpaEntity e) {
        if (e == null) {
            return null;
        }
        return Arc.reconstitute(e.getId(), e.getCampaignId(), e.getTitle(), e.getDescription(),
                e.getStatus(), e.getPosition(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
