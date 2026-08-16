package com.campaignorganizer.worldbuilding.adapter.wiki.out.persistence;

import com.campaignorganizer.worldbuilding.domain.wiki.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryPersistenceMapper {

    CategoryJpaEntity toEntity(Category category);

    default Category toDomain(CategoryJpaEntity e) {
        if (e == null) {
            return null;
        }
        return Category.reconstitute(e.getId(), e.getWorldId(), e.getParentId(), e.getName(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
