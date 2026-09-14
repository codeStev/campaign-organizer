package com.campaignorganizer.characters.adapter.template.out.persistence;

import com.campaignorganizer.characters.application.template.port.out.GlobalFieldTemplateRepositoryPort;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.characters.domain.template.GlobalFieldTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GlobalFieldTemplatePersistenceAdapter implements GlobalFieldTemplateRepositoryPort {

    private final GlobalFieldTemplateJpaRepository repository;
    private final GlobalFieldTemplatePersistenceMapper mapper;

    public GlobalFieldTemplatePersistenceAdapter(GlobalFieldTemplateJpaRepository repository,
                                                 GlobalFieldTemplatePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<GlobalFieldTemplate> findAllByOwnerId(UUID ownerId) {
        return repository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<GlobalFieldTemplate> findByOwnerIdAndKind(UUID ownerId, TemplateKind kind) {
        return repository.findByOwnerIdAndKindOrderByCreatedAtDesc(ownerId, kind).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Optional<GlobalFieldTemplate> findById(UUID templateId) {
        return repository.findById(templateId).map(mapper::toDomain);
    }

    @Override
    public Optional<GlobalFieldTemplate> findByKindAndSystemIdAndName(TemplateKind kind, UUID systemId,
                                                                       String name) {
        return repository.findByKindAndSystemIdAndName(kind, systemId, name).map(mapper::toDomain);
    }

    @Override
    public boolean existsBySystemId(UUID systemId) {
        return repository.existsBySystemId(systemId);
    }

    @Override
    public GlobalFieldTemplate save(GlobalFieldTemplate template) {
        return mapper.toDomain(repository.save(mapper.toEntity(template)));
    }

    @Override
    public void delete(GlobalFieldTemplate template) {
        repository.deleteById(template.getId());
    }

    @Override
    public void assignUnownedTo(UUID ownerId) {
        repository.assignUnownedTo(ownerId);
    }
}
