package com.campaignorganizer.campaign.adapter.arc.out.persistence;

import com.campaignorganizer.campaign.application.arc.port.out.ArcBeatRepositoryPort;
import com.campaignorganizer.campaign.domain.arc.ArcBeat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ArcBeatPersistenceAdapter implements ArcBeatRepositoryPort {

    private final ArcBeatJpaRepository repository;
    private final ArcBeatPersistenceMapper mapper;

    public ArcBeatPersistenceAdapter(ArcBeatJpaRepository repository, ArcBeatPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<ArcBeat> findByArc(UUID arcId) {
        return repository.findByArcIdOrderByPositionAscCreatedAtAsc(arcId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Optional<ArcBeat> findByIdAndArc(UUID beatId, UUID arcId) {
        return repository.findByIdAndArcId(beatId, arcId).map(mapper::toDomain);
    }

    @Override
    public List<ArcBeat> findBySession(UUID sessionId) {
        return repository.findBySessionIdOrderByPositionAscCreatedAtAsc(sessionId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<ArcBeat> findByLinkedArticle(UUID articleId) {
        return repository.findByLinkedArticleId(articleId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<UUID> linkedArticleIdsByArcs(Collection<UUID> arcIds) {
        return repository.findLinkedArticleIdsByArcIds(arcIds);
    }

    @Override
    public List<UUID> linkedStatblockIdsByArcs(Collection<UUID> arcIds) {
        return repository.findLinkedStatblockIdsByArcIds(arcIds);
    }

    @Override
    public ArcBeat save(ArcBeat beat) {
        return mapper.toDomain(repository.save(mapper.toEntity(beat)));
    }

    @Override
    public void delete(ArcBeat beat) {
        repository.deleteById(beat.getId());
    }
}
