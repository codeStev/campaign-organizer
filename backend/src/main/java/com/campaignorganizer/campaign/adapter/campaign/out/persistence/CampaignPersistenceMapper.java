package com.campaignorganizer.campaign.adapter.campaign.out.persistence;

import com.campaignorganizer.campaign.domain.campaign.Campaign;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CampaignPersistenceMapper {

    CampaignJpaEntity toEntity(Campaign campaign);

    default Campaign toDomain(CampaignJpaEntity e) {
        if (e == null) {
            return null;
        }
        return Campaign.reconstitute(e.getId(), e.getWorldId(), e.getName(), e.getDescription(),
                e.getNotes(), e.getStatus(), e.getSystemId(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
