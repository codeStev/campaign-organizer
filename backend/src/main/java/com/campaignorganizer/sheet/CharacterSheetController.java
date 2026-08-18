package com.campaignorganizer.sheet;

import com.campaignorganizer.campaign.CampaignRepository;
import com.campaignorganizer.sheet.CharacterSheetDtos.CharacterSheetRequest;
import com.campaignorganizer.sheet.CharacterSheetDtos.CharacterSheetResponse;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/worlds/{worldId}/character-sheets")
public class CharacterSheetController {

    private final CharacterSheetRepository sheets;
    private final SheetTemplateRepository templates;
    private final ArticleQueryPort articles;
    private final CampaignRepository campaigns;
    private final WorldQueryPort worlds;

    public CharacterSheetController(CharacterSheetRepository sheets, SheetTemplateRepository templates,
                                    ArticleQueryPort articles, CampaignRepository campaigns,
                                    WorldQueryPort worlds) {
        this.sheets = sheets;
        this.templates = templates;
        this.articles = articles;
        this.campaigns = campaigns;
        this.worlds = worlds;
    }

    @GetMapping
    public List<CharacterSheetResponse> list(@PathVariable UUID worldId,
                                             @RequestParam(required = false) UUID campaignId) {
        requireWorld(worldId);
        List<CharacterSheet> result = campaignId == null
                ? sheets.findByWorldIdOrderByCreatedAtDesc(worldId)
                : sheets.findByWorldIdAndCampaignIdOrderByCreatedAtDesc(worldId, campaignId);
        return result.stream().map(CharacterSheetResponse::from).toList();
    }

    @GetMapping("/{sheetId}")
    public CharacterSheetResponse get(@PathVariable UUID worldId, @PathVariable UUID sheetId) {
        return CharacterSheetResponse.from(findOrThrow(worldId, sheetId));
    }

    @PostMapping
    public ResponseEntity<CharacterSheetResponse> create(@PathVariable UUID worldId,
                                                         @Valid @RequestBody CharacterSheetRequest request) {
        requireWorld(worldId);
        validateLinks(worldId, request);
        CharacterSheet saved = sheets.save(new CharacterSheet(worldId, request.templateId(),
                request.articleId(), request.campaignId(), request.name(), request.values()));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/character-sheets/" + saved.getId()))
                .body(CharacterSheetResponse.from(saved));
    }

    @PutMapping("/{sheetId}")
    public CharacterSheetResponse update(@PathVariable UUID worldId, @PathVariable UUID sheetId,
                                         @Valid @RequestBody CharacterSheetRequest request) {
        CharacterSheet sheet = findOrThrow(worldId, sheetId);
        validateLinks(worldId, request);
        sheet.update(request.templateId(), request.articleId(), request.campaignId(),
                request.name(), request.values());
        return CharacterSheetResponse.from(sheets.save(sheet));
    }

    @DeleteMapping("/{sheetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID sheetId) {
        sheets.delete(findOrThrow(worldId, sheetId));
    }

    private CharacterSheet findOrThrow(UUID worldId, UUID sheetId) {
        return sheets.findByIdAndWorldId(sheetId, worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Character sheet not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "World not found");
        }
    }

    private void validateLinks(UUID worldId, CharacterSheetRequest request) {
        if (!templates.existsByIdAndWorldId(request.templateId(), worldId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template not found in this world");
        }
        if (request.articleId() != null && !articles.existsInWorld(request.articleId(), worldId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Article not found in this world");
        }
        if (request.campaignId() != null && !campaigns.existsByIdAndWorldId(request.campaignId(), worldId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign not found in this world");
        }
    }
}
