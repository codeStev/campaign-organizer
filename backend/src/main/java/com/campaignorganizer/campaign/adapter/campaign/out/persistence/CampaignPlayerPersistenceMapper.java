package com.campaignorganizer.campaign.adapter.campaign.out.persistence;

import com.campaignorganizer.campaign.domain.campaign.CampaignPlayer;
import org.mapstruct.Mapper;

/** Maps the domain aggregate to/from its JPA entity (MapStruct). */
@Mapper(componentModel = "spring")
public interface CampaignPlayerPersistenceMapper {

    CampaignPlayerJpaEntity toEntity(CampaignPlayer entry);

    /** The aggregate is immutable with a static factory, so reconstitute it explicitly. */
    default CampaignPlayer toDomain(CampaignPlayerJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return CampaignPlayer.reconstitute(entity.getId(), entity.getCampaignId(),
                entity.getPlayerId(), entity.isGuest(), entity.getCreatedAt());
    }
}
