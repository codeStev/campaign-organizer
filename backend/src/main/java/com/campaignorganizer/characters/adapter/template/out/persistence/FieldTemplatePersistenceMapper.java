package com.campaignorganizer.characters.adapter.template.out.persistence;

import com.campaignorganizer.characters.domain.template.FieldTemplate;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FieldTemplatePersistenceMapper {

    FieldTemplateJpaEntity toEntity(FieldTemplate template);

    default FieldTemplate toDomain(FieldTemplateJpaEntity e) {
        if (e == null) {
            return null;
        }
        return FieldTemplate.reconstitute(e.getId(), e.getWorldId(), e.getName(), e.getKind(), e.getSystem(),
                e.getSections(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
