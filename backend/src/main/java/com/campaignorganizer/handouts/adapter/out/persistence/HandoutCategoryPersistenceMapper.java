package com.campaignorganizer.handouts.adapter.out.persistence;

import com.campaignorganizer.handouts.domain.HandoutCategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HandoutCategoryPersistenceMapper {

    HandoutCategoryJpaEntity toEntity(HandoutCategory category);

    default HandoutCategory toDomain(HandoutCategoryJpaEntity e) {
        if (e == null) {
            return null;
        }
        return HandoutCategory.reconstitute(e.getId(), e.getWorldId(), e.getParentId(), e.getName(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
