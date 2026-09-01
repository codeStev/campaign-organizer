package com.campaignorganizer.characters.application.document.port.out;

import com.campaignorganizer.characters.domain.document.Document;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepositoryPort {

    List<Document> findByWorld(UUID worldId);

    List<Document> findByWorldAndCampaign(UUID worldId, UUID campaignId);

    Optional<Document> findByIdAndWorld(UUID documentId, UUID worldId);

    Document save(Document document);

    void delete(Document document);
}
