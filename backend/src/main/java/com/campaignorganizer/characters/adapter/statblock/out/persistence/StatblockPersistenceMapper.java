package com.campaignorganizer.characters.adapter.statblock.out.persistence;

import com.campaignorganizer.characters.domain.statblock.Statblock;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StatblockPersistenceMapper {

    StatblockJpaEntity toEntity(Statblock statblock);

    default Statblock toDomain(StatblockJpaEntity e) {
        if (e == null) {
            return null;
        }
        return Statblock.reconstitute(e.getId(), e.getWorldId(), e.getArticleId(), e.getCampaignId(),
                e.getName(), e.getStats(), e.getNotes(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
