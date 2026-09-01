package com.campaignorganizer.characters.adapter.template.out.persistence;

import com.campaignorganizer.characters.domain.template.GlobalFieldTemplate;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GlobalFieldTemplatePersistenceMapper {

    GlobalFieldTemplateJpaEntity toEntity(GlobalFieldTemplate template);

    default GlobalFieldTemplate toDomain(GlobalFieldTemplateJpaEntity e) {
        if (e == null) {
            return null;
        }
        return GlobalFieldTemplate.reconstitute(e.getId(), e.getName(), e.getKind(), e.getSystemId(),
                e.getSections(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
