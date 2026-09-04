package com.campaignorganizer.characters.application.document.service;

import com.campaignorganizer.characters.application.document.port.in.CreateDocumentUseCase;
import com.campaignorganizer.characters.application.document.port.in.DeleteDocumentUseCase;
import com.campaignorganizer.characters.application.document.port.in.DocumentCommands.CreateDocumentCommand;
import com.campaignorganizer.characters.application.document.port.in.DocumentCommands.UpdateDocumentCommand;
import com.campaignorganizer.characters.application.document.port.in.GetDocumentUseCase;
import com.campaignorganizer.characters.application.document.port.in.ListDocumentsUseCase;
import com.campaignorganizer.characters.application.document.port.in.UpdateDocumentUseCase;
import com.campaignorganizer.characters.application.document.port.out.CampaignExistsPort;
import com.campaignorganizer.characters.application.document.port.out.DocumentRepositoryPort;
import com.campaignorganizer.characters.application.document.port.out.WorldExistsPort;
import com.campaignorganizer.characters.application.document.port.published.DocumentImportPort;
import com.campaignorganizer.characters.application.document.port.published.DocumentQueryPort;
import com.campaignorganizer.characters.application.document.port.published.DocumentView;
import com.campaignorganizer.characters.application.category.port.published.SheetCategoryQueryPort;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateQueryPort;
import com.campaignorganizer.characters.domain.document.Document;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Document use cases; also implements the published query/import ports for consumers. */
@Service
public class DocumentService implements CreateDocumentUseCase, UpdateDocumentUseCase,
        DeleteDocumentUseCase, GetDocumentUseCase, ListDocumentsUseCase, DocumentQueryPort,
        DocumentImportPort {

    private final DocumentRepositoryPort documents;
    private final FieldTemplateQueryPort templates;
    private final WorldExistsPort worlds;
    private final CampaignExistsPort campaigns;
    private final SheetCategoryQueryPort categories;
    private final DocumentViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public DocumentService(DocumentRepositoryPort documents, FieldTemplateQueryPort templates,
                           WorldExistsPort worlds, CampaignExistsPort campaigns,
                           SheetCategoryQueryPort categories,
                           DocumentViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.documents = documents;
        this.templates = templates;
        this.worlds = worlds;
        this.campaigns = campaigns;
        this.categories = categories;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public DocumentView create(CreateDocumentCommand command) {
        requireWorld(command.worldId());
        validateLinks(command.worldId(), command.templateId(), command.campaignId());
        validateCategory(command.worldId(), command.categoryId());
        Document created = Document.create(ids.newId(), command.worldId(), command.categoryId(),
                command.templateId(), command.campaignId(), command.name(), command.values(),
                clock.instant());
        return viewMapper.toView(documents.save(created));
    }

    @Override
    @Transactional
    public DocumentView update(UpdateDocumentCommand command) {
        Document document = require(command.worldId(), command.documentId());
        validateLinks(command.worldId(), command.templateId(), command.campaignId());
        validateCategory(command.worldId(), command.categoryId());
        document.update(command.categoryId(), command.templateId(), command.campaignId(), command.name(),
                command.values(), clock.instant());
        return viewMapper.toView(documents.save(document));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID documentId) {
        documents.delete(require(worldId, documentId));
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentView get(UUID worldId, UUID documentId) {
        return viewMapper.toView(require(worldId, documentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentView> list(UUID worldId, UUID campaignId) {
        requireWorld(worldId);
        List<Document> result = campaignId == null
                ? documents.findByWorld(worldId)
                : documents.findByWorldAndCampaign(worldId, campaignId);
        return result.stream().map(viewMapper::toView).toList();
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public DocumentView importDocument(DocumentView view) {
        Document document = Document.reconstitute(view.id(), view.worldId(), view.categoryId(),
                view.templateId(), view.campaignId(), view.name(), view.values(), view.createdAt(),
                view.updatedAt());
        return viewMapper.toView(documents.save(document));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<DocumentView> findByWorld(UUID worldId) {
        return documents.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    private Document require(UUID worldId, UUID documentId) {
        return documents.findByIdAndWorld(documentId, worldId)
                .orElseThrow(() -> new NotFoundException("Document not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }

    private void validateLinks(UUID worldId, UUID templateId, UUID campaignId) {
        TemplateKind kind = templates.findByIdInWorld(templateId, worldId)
                .orElseThrow(() -> new ValidationException("Template not found in this world"))
                .kind();
        if (kind != TemplateKind.DOCUMENT) {
            throw new ValidationException("Template is not a document template");
        }
        if (campaignId != null && !campaigns.existsInWorld(campaignId, worldId)) {
            throw new ValidationException("Campaign not found in this world");
        }
    }

    private void validateCategory(UUID worldId, UUID categoryId) {
        if (categoryId != null && !categories.existsInWorld(categoryId, worldId)) {
            throw new ValidationException("Category not found in this world");
        }
    }
}
