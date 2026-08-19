package com.campaignorganizer.characters.adapter.sheet.out.persistence;

import com.campaignorganizer.characters.domain.sheet.SheetTemplate;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SheetTemplatePersistenceMapper {

    SheetTemplateJpaEntity toEntity(SheetTemplate template);

    default SheetTemplate toDomain(SheetTemplateJpaEntity e) {
        if (e == null) {
            return null;
        }
        return SheetTemplate.reconstitute(e.getId(), e.getWorldId(), e.getName(), e.getSystem(),
                e.getSections(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
