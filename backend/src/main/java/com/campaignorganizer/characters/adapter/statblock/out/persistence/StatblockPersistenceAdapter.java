package com.campaignorganizer.characters.adapter.statblock.out.persistence;

import com.campaignorganizer.characters.application.statblock.port.out.StatblockRepositoryPort;
import com.campaignorganizer.characters.domain.statblock.Statblock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StatblockPersistenceAdapter implements StatblockRepositoryPort {

    private final StatblockJpaRepository repository;
    private final StatblockPersistenceMapper mapper;

    public StatblockPersistenceAdapter(StatblockJpaRepository repository,
                                       StatblockPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Statblock> findByWorld(UUID worldId) {
        return repository.findByWorldIdOrderByCreatedAtDesc(worldId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Statblock> findByWorldAndCampaign(UUID worldId, UUID campaignId) {
        return repository.findByWorldIdAndCampaignIdOrderByCreatedAtDesc(worldId, campaignId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Statblock> findByWorldAndArticle(UUID worldId, UUID articleId) {
        return repository.findByWorldIdAndArticleId(worldId, articleId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Statblock> findAllByIds(Collection<UUID> ids) {
        return repository.findAllById(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Statblock> findByIdAndWorld(UUID statblockId, UUID worldId) {
        return repository.findByIdAndWorldId(statblockId, worldId).map(mapper::toDomain);
    }

    @Override
    public Statblock save(Statblock statblock) {
        return mapper.toDomain(repository.save(mapper.toEntity(statblock)));
    }

    @Override
    public void delete(Statblock statblock) {
        repository.deleteById(statblock.getId());
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
