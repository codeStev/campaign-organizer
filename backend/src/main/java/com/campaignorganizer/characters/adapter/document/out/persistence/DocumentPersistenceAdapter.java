package com.campaignorganizer.characters.adapter.document.out.persistence;

import com.campaignorganizer.characters.application.document.port.out.DocumentRepositoryPort;
import com.campaignorganizer.characters.domain.document.Document;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DocumentPersistenceAdapter implements DocumentRepositoryPort {

    private final DocumentJpaRepository repository;
    private final DocumentPersistenceMapper mapper;

    public DocumentPersistenceAdapter(DocumentJpaRepository repository, DocumentPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Document> findByWorld(UUID worldId) {
        return repository.findByWorldIdOrderByCreatedAtDesc(worldId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Document> findByWorldAndCampaign(UUID worldId, UUID campaignId) {
        return repository.findByWorldIdAndCampaignIdOrderByCreatedAtDesc(worldId, campaignId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Document> findByIdAndWorld(UUID documentId, UUID worldId) {
        return repository.findByIdAndWorldId(documentId, worldId).map(mapper::toDomain);
    }

    @Override
    public Document save(Document document) {
        return mapper.toDomain(repository.save(mapper.toEntity(document)));
    }

    @Override
    public void delete(Document document) {
        repository.deleteById(document.getId());
    }
}
