package com.campaignorganizer.ai.adapter.out.persistence;

import com.campaignorganizer.ai.domain.ProviderSetting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps the domain setting to/from its JPA entity (MapStruct). {@code updatedAt}, {@code id}, and
 * {@code ownerId} are persistence-only (not domain concerns), so the adapter stamps them separately. */
@Mapper(componentModel = "spring")
public interface AiProviderSettingsPersistenceMapper {

    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    AiProviderSettingsJpaEntity toEntity(ProviderSetting setting);

    default ProviderSetting toDomain(AiProviderSettingsJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ProviderSetting(entity.getProviderId(), entity.getModel(), entity.getPriority());
    }
}
