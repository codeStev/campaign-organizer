package com.campaignorganizer.interchange.foundry.adapter.out.persistence;

import com.campaignorganizer.interchange.foundry.domain.FoundryEntityType;
import com.campaignorganizer.interchange.foundry.domain.FoundryPushRecord;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FoundryPushRecordPersistenceMapper {

    FoundryPushRecordJpaEntity toEntity(FoundryPushRecord record);

    default FoundryPushRecord toDomain(FoundryPushRecordJpaEntity e) {
        if (e == null) {
            return null;
        }
        return FoundryPushRecord.reconstitute(e.getId(), e.getWorldId(),
                FoundryEntityType.valueOf(e.getEntityType()), e.getEntityId(), e.getFoundryDocumentId(),
                e.getPushedAt());
    }
}
