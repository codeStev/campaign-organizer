package com.campaignorganizer.characters.adapter.sheet.out.persistence;

import com.campaignorganizer.characters.application.sheet.port.out.SheetTemplateRepositoryPort;
import com.campaignorganizer.characters.domain.sheet.SheetTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SheetTemplatePersistenceAdapter implements SheetTemplateRepositoryPort {

    private final SheetTemplateJpaRepository repository;
    private final SheetTemplatePersistenceMapper mapper;

    public SheetTemplatePersistenceAdapter(SheetTemplateJpaRepository repository,
                                           SheetTemplatePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<SheetTemplate> findByWorld(UUID worldId) {
        return repository.findByWorldIdOrderByCreatedAtDesc(worldId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<SheetTemplate> findByIdAndWorld(UUID templateId, UUID worldId) {
        return repository.findByIdAndWorldId(templateId, worldId).map(mapper::toDomain);
    }

    @Override
    public boolean existsInWorld(UUID templateId, UUID worldId) {
        return repository.existsByIdAndWorldId(templateId, worldId);
    }

    @Override
    public SheetTemplate save(SheetTemplate template) {
        return mapper.toDomain(repository.save(mapper.toEntity(template)));
    }

    @Override
    public void delete(SheetTemplate template) {
        repository.deleteById(template.getId());
    }
}
