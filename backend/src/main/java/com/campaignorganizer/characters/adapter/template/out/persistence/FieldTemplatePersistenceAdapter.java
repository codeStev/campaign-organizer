package com.campaignorganizer.characters.adapter.template.out.persistence;

import com.campaignorganizer.characters.application.template.port.out.FieldTemplateRepositoryPort;
import com.campaignorganizer.characters.domain.template.FieldTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class FieldTemplatePersistenceAdapter implements FieldTemplateRepositoryPort {

    private final FieldTemplateJpaRepository repository;
    private final FieldTemplatePersistenceMapper mapper;

    public FieldTemplatePersistenceAdapter(FieldTemplateJpaRepository repository,
                                           FieldTemplatePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<FieldTemplate> findByWorld(UUID worldId) {
        return repository.findByWorldIdOrderByCreatedAtDesc(worldId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<FieldTemplate> findByIdAndWorld(UUID templateId, UUID worldId) {
        return repository.findByIdAndWorldId(templateId, worldId).map(mapper::toDomain);
    }

    @Override
    public boolean existsInWorld(UUID templateId, UUID worldId) {
        return repository.existsByIdAndWorldId(templateId, worldId);
    }

    @Override
    public FieldTemplate save(FieldTemplate template) {
        return mapper.toDomain(repository.save(mapper.toEntity(template)));
    }

    @Override
    public void delete(FieldTemplate template) {
        repository.deleteById(template.getId());
    }
}
