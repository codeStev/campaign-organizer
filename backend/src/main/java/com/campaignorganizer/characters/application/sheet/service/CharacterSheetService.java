package com.campaignorganizer.characters.application.sheet.service;

import com.campaignorganizer.characters.application.sheet.port.in.CharacterSheetCommands.CreateCharacterSheetCommand;
import com.campaignorganizer.characters.application.sheet.port.in.CharacterSheetCommands.UpdateCharacterSheetCommand;
import com.campaignorganizer.characters.application.sheet.port.in.CreateCharacterSheetUseCase;
import com.campaignorganizer.characters.application.sheet.port.in.DeleteCharacterSheetUseCase;
import com.campaignorganizer.characters.application.sheet.port.in.GetCharacterSheetUseCase;
import com.campaignorganizer.characters.application.sheet.port.in.ListCharacterSheetsUseCase;
import com.campaignorganizer.characters.application.sheet.port.in.UpdateCharacterSheetUseCase;
import com.campaignorganizer.characters.application.sheet.port.out.ArticleExistsPort;
import com.campaignorganizer.characters.application.sheet.port.out.CampaignExistsPort;
import com.campaignorganizer.characters.application.sheet.port.out.CharacterSheetRepositoryPort;
import com.campaignorganizer.characters.application.sheet.port.out.WorldExistsPort;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetImportPort;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetQueryPort;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetTemplateRefPort;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetView;
import com.campaignorganizer.characters.application.category.port.published.SheetCategoryQueryPort;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateQueryPort;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateQueryPort;
import com.campaignorganizer.characters.domain.sheet.CharacterSheet;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Character sheet use cases; also implements the published query port for consumers. */
@Service
public class CharacterSheetService implements CreateCharacterSheetUseCase, UpdateCharacterSheetUseCase,
        DeleteCharacterSheetUseCase, GetCharacterSheetUseCase, ListCharacterSheetsUseCase,
        CharacterSheetQueryPort, CharacterSheetImportPort, CharacterSheetTemplateRefPort {

    private final CharacterSheetRepositoryPort sheets;
    private final FieldTemplateQueryPort templates;
    private final GlobalFieldTemplateQueryPort globalTemplates;
    private final WorldExistsPort worlds;
    private final ArticleExistsPort articles;
    private final CampaignExistsPort campaigns;
    private final SheetCategoryQueryPort categories;
    private final CharacterSheetViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public CharacterSheetService(CharacterSheetRepositoryPort sheets, FieldTemplateQueryPort templates,
                                 GlobalFieldTemplateQueryPort globalTemplates, WorldExistsPort worlds,
                                 ArticleExistsPort articles, CampaignExistsPort campaigns,
                                 SheetCategoryQueryPort categories,
                                 CharacterSheetViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.sheets = sheets;
        this.templates = templates;
        this.globalTemplates = globalTemplates;
        this.worlds = worlds;
        this.articles = articles;
        this.campaigns = campaigns;
        this.categories = categories;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CharacterSheetView create(CreateCharacterSheetCommand command) {
        requireWorld(command.worldId());
        validateLinks(command.worldId(), command.worldTemplateId(), command.globalTemplateId(),
                command.articleId(), command.campaignId());
        validateCategory(command.worldId(), command.categoryId());
        CharacterSheet created = CharacterSheet.create(ids.newId(), command.worldId(), command.categoryId(),
                command.worldTemplateId(), command.globalTemplateId(), command.articleId(),
                command.campaignId(), command.name(), command.values(), clock.instant());
        return viewMapper.toView(sheets.save(created));
    }

    @Override
    @Transactional
    public CharacterSheetView update(UpdateCharacterSheetCommand command) {
        CharacterSheet sheet = require(command.worldId(), command.sheetId());
        validateLinks(command.worldId(), command.worldTemplateId(), command.globalTemplateId(),
                command.articleId(), command.campaignId());
        validateCategory(command.worldId(), command.categoryId());
        sheet.update(command.categoryId(), command.worldTemplateId(), command.globalTemplateId(),
                command.articleId(), command.campaignId(), command.name(), command.values(), clock.instant());
        return viewMapper.toView(sheets.save(sheet));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID sheetId) {
        sheets.delete(require(worldId, sheetId));
    }

    @Override
    @Transactional(readOnly = true)
    public CharacterSheetView get(UUID worldId, UUID sheetId) {
        return viewMapper.toView(require(worldId, sheetId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CharacterSheetView> list(UUID worldId, UUID campaignId) {
        requireWorld(worldId);
        List<CharacterSheet> result = campaignId == null
                ? sheets.findByWorld(worldId)
                : sheets.findByWorldAndCampaign(worldId, campaignId);
        return result.stream().map(viewMapper::toView).toList();
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public CharacterSheetView importCharacterSheet(CharacterSheetView view) {
        CharacterSheet sheet = CharacterSheet.reconstitute(view.id(), view.worldId(), view.categoryId(),
                view.worldTemplateId(), view.globalTemplateId(), view.articleId(), view.campaignId(),
                view.name(), view.values(), view.createdAt(), view.updatedAt());
        return viewMapper.toView(sheets.save(sheet));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<CharacterSheetView> findByWorld(UUID worldId) {
        return sheets.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CharacterSheetView> findByWorldAndCampaign(UUID worldId, UUID campaignId) {
        return sheets.findByWorldAndCampaign(worldId, campaignId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CharacterSheetView> findByWorldAndArticle(UUID worldId, UUID articleId) {
        return sheets.findByWorldAndArticle(worldId, articleId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CharacterSheetView> findByIdInWorld(UUID sheetId, UUID worldId) {
        return sheets.findByIdAndWorld(sheetId, worldId).map(viewMapper::toView);
    }

    private CharacterSheet require(UUID worldId, UUID sheetId) {
        return sheets.findByIdAndWorld(sheetId, worldId)
                .orElseThrow(() -> new NotFoundException("Character sheet not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }

    private void validateLinks(UUID worldId, UUID worldTemplateId, UUID globalTemplateId, UUID articleId,
                               UUID campaignId) {
        TemplateKind kind;
        if (worldTemplateId != null) {
            kind = templates.findByIdInWorld(worldTemplateId, worldId)
                    .orElseThrow(() -> new ValidationException("Template not found in this world"))
                    .kind();
        } else if (globalTemplateId != null) {
            kind = globalTemplates.findById(globalTemplateId)
                    .orElseThrow(() -> new ValidationException("Global template not found"))
                    .kind();
        } else {
            throw new ValidationException("Character sheet requires a template");
        }
        if (kind != TemplateKind.CHARACTER) {
            throw new ValidationException("Template is not a character sheet template");
        }
        if (articleId != null && !articles.existsInWorld(articleId, worldId)) {
            throw new ValidationException("Article not found in this world");
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

    // --- published template-ref port (ADR-0093, used by template promotion) ---

    @Override
    @Transactional
    public void repointWorldTemplateToGlobal(UUID worldTemplateId, UUID globalTemplateId) {
        sheets.repointWorldTemplateToGlobal(worldTemplateId, globalTemplateId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsReferencingGlobalTemplate(UUID globalTemplateId) {
        return sheets.existsByGlobalTemplateId(globalTemplateId);
    }
}
