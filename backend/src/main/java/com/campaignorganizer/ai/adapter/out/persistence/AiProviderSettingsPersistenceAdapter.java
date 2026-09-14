package com.campaignorganizer.ai.adapter.out.persistence;

import com.campaignorganizer.ai.application.port.out.AiProviderSettingsRepositoryPort;
import com.campaignorganizer.ai.domain.ProviderSetting;
import com.campaignorganizer.shared.application.IdGenerator;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** JPA-backed implementation of the settings repository port. */
@Component
public class AiProviderSettingsPersistenceAdapter implements AiProviderSettingsRepositoryPort {

    private final AiProviderSettingsJpaRepository repository;
    private final AiProviderSettingsPersistenceMapper mapper;
    private final IdGenerator ids;
    private final Clock clock;

    public AiProviderSettingsPersistenceAdapter(AiProviderSettingsJpaRepository repository,
                                                AiProviderSettingsPersistenceMapper mapper, IdGenerator ids,
                                                Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    public List<ProviderSetting> findAllOrderedByPriority(UUID ownerId) {
        return repository.findAllByOwnerIdOrderByPriorityAsc(ownerId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void replaceAll(UUID ownerId, List<ProviderSetting> settings) {
        repository.deleteAllByOwnerId(ownerId);
        List<AiProviderSettingsJpaEntity> entities = settings.stream().map(setting -> {
            AiProviderSettingsJpaEntity entity = mapper.toEntity(setting);
            entity.setId(ids.newId());
            entity.setOwnerId(ownerId);
            entity.setUpdatedAt(clock.instant());
            return entity;
        }).toList();
        repository.saveAll(entities);
    }

    @Override
    public void assignUnownedTo(UUID ownerId) {
        repository.assignUnownedTo(ownerId);
    }
}
