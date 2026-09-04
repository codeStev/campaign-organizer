package com.campaignorganizer.tables.adapter.category.out.persistence;

import com.campaignorganizer.tables.domain.category.TableDeckCategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TableDeckCategoryPersistenceMapper {

    TableDeckCategoryJpaEntity toEntity(TableDeckCategory category);

    default TableDeckCategory toDomain(TableDeckCategoryJpaEntity e) {
        if (e == null) {
            return null;
        }
        return TableDeckCategory.reconstitute(e.getId(), e.getWorldId(), e.getParentId(), e.getName(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
