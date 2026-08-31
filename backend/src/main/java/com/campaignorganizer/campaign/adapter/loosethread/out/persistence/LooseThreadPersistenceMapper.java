package com.campaignorganizer.campaign.adapter.loosethread.out.persistence;

import com.campaignorganizer.campaign.domain.loosethread.LooseThread;
import com.campaignorganizer.campaign.domain.loosethread.LooseThreadStatus;
import org.mapstruct.Mapper;

/** Maps the domain aggregate to/from its JPA entity (MapStruct). */
@Mapper(componentModel = "spring")
public interface LooseThreadPersistenceMapper {

    LooseThreadJpaEntity toEntity(LooseThread thread);

    /** The aggregate is immutable with a static factory, so reconstitute it explicitly. */
    default LooseThread toDomain(LooseThreadJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return LooseThread.reconstitute(
                entity.getId(),
                entity.getSessionId(),
                entity.getCampaignId(),
                entity.getText(),
                LooseThreadStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
