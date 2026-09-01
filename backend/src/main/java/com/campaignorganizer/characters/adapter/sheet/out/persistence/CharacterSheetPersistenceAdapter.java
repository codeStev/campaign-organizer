package com.campaignorganizer.characters.adapter.sheet.out.persistence;

import com.campaignorganizer.characters.application.sheet.port.out.CharacterSheetRepositoryPort;
import com.campaignorganizer.characters.domain.sheet.CharacterSheet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CharacterSheetPersistenceAdapter implements CharacterSheetRepositoryPort {

    private final CharacterSheetJpaRepository repository;
    private final CharacterSheetPersistenceMapper mapper;

    public CharacterSheetPersistenceAdapter(CharacterSheetJpaRepository repository,
                                            CharacterSheetPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<CharacterSheet> findByWorld(UUID worldId) {
        return repository.findByWorldIdOrderByCreatedAtDesc(worldId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<CharacterSheet> findByWorldAndCampaign(UUID worldId, UUID campaignId) {
        return repository.findByWorldIdAndCampaignIdOrderByCreatedAtDesc(worldId, campaignId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<CharacterSheet> findByWorldAndArticle(UUID worldId, UUID articleId) {
        return repository.findByWorldIdAndArticleId(worldId, articleId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Optional<CharacterSheet> findByIdAndWorld(UUID sheetId, UUID worldId) {
        return repository.findByIdAndWorldId(sheetId, worldId).map(mapper::toDomain);
    }

    @Override
    public CharacterSheet save(CharacterSheet sheet) {
        return mapper.toDomain(repository.save(mapper.toEntity(sheet)));
    }

    @Override
    public void delete(CharacterSheet sheet) {
        repository.deleteById(sheet.getId());
    }

    @Override
    public void repointWorldTemplateToGlobal(UUID worldTemplateId, UUID globalTemplateId) {
        repository.repointWorldTemplateToGlobal(worldTemplateId, globalTemplateId);
    }

    @Override
    public boolean existsByGlobalTemplateId(UUID globalTemplateId) {
        return repository.existsByGlobalTemplateId(globalTemplateId);
    }
}
