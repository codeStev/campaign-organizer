package com.campaignorganizer.characters.application.statblock.service;

import com.campaignorganizer.characters.application.statblock.port.in.CreateStatblockUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.DeleteStatblockUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.GetStatblockUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.ListStatblocksUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.StatblockCommands.CreateStatblockCommand;
import com.campaignorganizer.characters.application.statblock.port.in.StatblockCommands.UpdateStatblockCommand;
import com.campaignorganizer.characters.application.statblock.port.in.UpdateStatblockUseCase;
import com.campaignorganizer.characters.application.statblock.port.out.ArticleExistsPort;
import com.campaignorganizer.characters.application.statblock.port.out.CampaignExistsPort;
import com.campaignorganizer.characters.application.statblock.port.out.CampaignStatblockRefPort;
import com.campaignorganizer.characters.application.statblock.port.out.StatblockRepositoryPort;
import com.campaignorganizer.characters.application.statblock.port.out.StatblockTagLookupPort;
import com.campaignorganizer.characters.application.statblock.port.out.WorldExistsPort;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockImportPort;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockQueryPort;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateQueryPort;
import com.campaignorganizer.characters.domain.statblock.Statblock;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Statblock use cases; also implements the published query port for consumers. */
@Service
public class StatblockService implements CreateStatblockUseCase, UpdateStatblockUseCase,
        DeleteStatblockUseCase, GetStatblockUseCase, ListStatblocksUseCase, StatblockQueryPort,
        StatblockImportPort {

    private final StatblockRepositoryPort statblocks;
    private final WorldExistsPort worlds;
    private final ArticleExistsPort articles;
    private final CampaignExistsPort campaigns;
    private final FieldTemplateQueryPort templates;
    private final CampaignStatblockRefPort campaignRefs;
    private final StatblockTagLookupPort tagLookup;
    private final StatblockViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public StatblockService(StatblockRepositoryPort statblocks, WorldExistsPort worlds,
                            ArticleExistsPort articles, CampaignExistsPort campaigns,
                            FieldTemplateQueryPort templates, CampaignStatblockRefPort campaignRefs,
                            StatblockTagLookupPort tagLookup, StatblockViewMapper viewMapper,
                            IdGenerator ids, Clock clock) {
        this.statblocks = statblocks;
        this.worlds = worlds;
        this.articles = articles;
        this.campaigns = campaigns;
        this.templates = templates;
        this.campaignRefs = campaignRefs;
        this.tagLookup = tagLookup;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public StatblockView create(CreateStatblockCommand command) {
        requireWorld(command.worldId());
        validateLinks(command.worldId(), command.articleId(), command.campaignId(), command.templateId());
        Statblock created = Statblock.create(ids.newId(), command.worldId(), command.articleId(),
                command.campaignId(), command.templateId(), command.name(), command.stats(), command.notes(),
                clock.instant());
        return viewMapper.toView(statblocks.save(created));
    }

    @Override
    @Transactional
    public StatblockView update(UpdateStatblockCommand command) {
        Statblock statblock = require(command.worldId(), command.statblockId());
        validateLinks(command.worldId(), command.articleId(), command.campaignId(), command.templateId());
        statblock.update(command.articleId(), command.campaignId(), command.templateId(), command.name(),
                command.stats(), command.notes(), clock.instant());
        return viewMapper.toView(statblocks.save(statblock));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID statblockId) {
        statblocks.delete(require(worldId, statblockId));
    }

    @Override
    @Transactional(readOnly = true)
    public StatblockView get(UUID worldId, UUID statblockId) {
        return viewMapper.toView(require(worldId, statblockId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatblockView> list(UUID worldId, UUID campaignId, String tag) {
        requireWorld(worldId);
        List<Statblock> result = campaignId == null
                ? statblocks.findByWorld(worldId)
                : statblocksForCampaign(worldId, campaignId);
        if (tag != null && !tag.isBlank()) {
            Set<UUID> tagged = tagLookup.statblockIdsTaggedWith(worldId, tag);
            result = result.stream().filter(s -> tagged.contains(s.getId())).toList();
        }
        return result.stream().map(viewMapper::toView).toList();
    }

    /**
     * Statblocks relevant to a campaign (ADR-0043): those explicitly scoped to it,
     * plus any (even shared) statblock referenced by one of the campaign's beats.
     */
    private List<Statblock> statblocksForCampaign(UUID worldId, UUID campaignId) {
        Map<UUID, Statblock> byId = new LinkedHashMap<>();
        statblocks.findByWorldAndCampaign(worldId, campaignId).forEach(s -> byId.put(s.getId(), s));

        List<UUID> refIds = campaignRefs.statblockIdsReferencedByCampaign(campaignId);
        if (!refIds.isEmpty()) {
            statblocks.findAllByIds(refIds).forEach(s -> {
                if (s.getWorldId().equals(worldId)) {
                    byId.putIfAbsent(s.getId(), s);
                }
            });
        }
        return new ArrayList<>(byId.values());
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public StatblockView importStatblock(StatblockView view) {
        Statblock statblock = Statblock.reconstitute(view.id(), view.worldId(), view.articleId(),
                view.campaignId(), view.templateId(), view.name(), view.stats(), view.notes(),
                view.createdAt(), view.updatedAt());
        return viewMapper.toView(statblocks.save(statblock));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<StatblockView> findByWorld(UUID worldId) {
        return statblocks.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatblockView> findByWorldAndCampaign(UUID worldId, UUID campaignId) {
        return statblocks.findByWorldAndCampaign(worldId, campaignId).stream()
                .map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatblockView> findByWorldAndArticle(UUID worldId, UUID articleId) {
        return statblocks.findByWorldAndArticle(worldId, articleId).stream()
                .map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StatblockView> findByIdInWorld(UUID statblockId, UUID worldId) {
        return statblocks.findByIdAndWorld(statblockId, worldId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInWorld(UUID statblockId, UUID worldId) {
        return statblocks.findByIdAndWorld(statblockId, worldId).isPresent();
    }

    private Statblock require(UUID worldId, UUID statblockId) {
        return statblocks.findByIdAndWorld(statblockId, worldId)
                .orElseThrow(() -> new NotFoundException("Statblock not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }

    private void validateLinks(UUID worldId, UUID articleId, UUID campaignId, UUID templateId) {
        if (articleId != null && !articles.existsInWorld(articleId, worldId)) {
            throw new ValidationException("Article not found in this world");
        }
        if (campaignId != null && !campaigns.existsInWorld(campaignId, worldId)) {
            throw new ValidationException("Campaign not found in this world");
        }
        if (templateId != null) {
            TemplateKind kind = templates.findByIdInWorld(templateId, worldId)
                    .orElseThrow(() -> new ValidationException("Template not found in this world"))
                    .kind();
            if (kind != TemplateKind.STATBLOCK) {
                throw new ValidationException("Template is not a statblock template");
            }
        }
    }
}
