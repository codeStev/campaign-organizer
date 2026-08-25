package com.campaignorganizer.ai.adapter.out.persistence;

import com.campaignorganizer.ai.application.port.out.AiProviderSettingsRepositoryPort;
import com.campaignorganizer.ai.domain.ProviderSetting;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Component;

/** JPA-backed implementation of the settings repository port. */
@Component
public class AiProviderSettingsPersistenceAdapter implements AiProviderSettingsRepositoryPort {

    private final AiProviderSettingsJpaRepository repository;
    private final AiProviderSettingsPersistenceMapper mapper;
    private final Clock clock;

    public AiProviderSettingsPersistenceAdapter(
            AiProviderSettingsJpaRepository repository, AiProviderSettingsPersistenceMapper mapper, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public List<ProviderSetting> findAllOrderedByPriority() {
        return repository.findAllByOrderByPriorityAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void replaceAll(List<ProviderSetting> settings) {
        repository.deleteAll();
        List<AiProviderSettingsJpaEntity> entities = settings.stream().map(setting -> {
            AiProviderSettingsJpaEntity entity = mapper.toEntity(setting);
            entity.setUpdatedAt(clock.instant());
            return entity;
        }).toList();
        repository.saveAll(entities);
    }
}
