package com.campaignorganizer.characters.adapter.statblock.out.persistence;

import com.campaignorganizer.characters.domain.statblock.GlobalStatblock;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GlobalStatblockPersistenceMapper {

    GlobalStatblockJpaEntity toEntity(GlobalStatblock statblock);

    default GlobalStatblock toDomain(GlobalStatblockJpaEntity e) {
        if (e == null) {
            return null;
        }
        return GlobalStatblock.reconstitute(e.getId(), e.getSystemId(), e.getGlobalTemplateId(), e.getName(),
                e.getStats(), e.getNotes(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
