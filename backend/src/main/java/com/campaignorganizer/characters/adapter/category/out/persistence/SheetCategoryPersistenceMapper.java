package com.campaignorganizer.characters.adapter.category.out.persistence;

import com.campaignorganizer.characters.domain.category.SheetCategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SheetCategoryPersistenceMapper {

    SheetCategoryJpaEntity toEntity(SheetCategory category);

    default SheetCategory toDomain(SheetCategoryJpaEntity e) {
        if (e == null) {
            return null;
        }
        return SheetCategory.reconstitute(e.getId(), e.getWorldId(), e.getParentId(), e.getName(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
