package com.campaignorganizer.interchange.foundry.adapter.in.web;

import com.campaignorganizer.interchange.foundry.adapter.in.web.FoundryPushWebDtos.FoundryCampaignPushResponse;
import com.campaignorganizer.interchange.foundry.adapter.in.web.FoundryPushWebDtos.FoundryCategoryPushResponse;
import com.campaignorganizer.interchange.foundry.adapter.in.web.FoundryPushWebDtos.FoundryPushResponse;
import com.campaignorganizer.interchange.foundry.adapter.in.web.FoundryPushWebDtos.FoundryPushStatusResponse;
import com.campaignorganizer.interchange.foundry.adapter.in.web.FoundryPushWebDtos.FoundrySessionPushResponse;
import com.campaignorganizer.interchange.foundry.application.port.in.GetFoundryPushStatusUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.PushArticleToFoundryUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.PushCampaignToFoundryUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.PushCardDeckToFoundryUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.PushCategoryToFoundryUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.PushHandoutToFoundryUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.PushRollTableToFoundryUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.PushSessionToFoundryUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.PushWorldWikiToFoundryUseCase;
import com.campaignorganizer.interchange.foundry.domain.FoundryCategoryPushMode;
import com.campaignorganizer.interchange.foundry.domain.FoundryEntityType;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Push Campaign Organizer content to Foundry VTT (ADR-0115). */
@RestController
@RequestMapping("/api/worlds/{worldId}/foundry")
public class FoundryPushController {

    private final PushArticleToFoundryUseCase pushArticleUseCase;
    private final PushHandoutToFoundryUseCase pushHandoutUseCase;
    private final PushRollTableToFoundryUseCase pushRollTableUseCase;
    private final PushCardDeckToFoundryUseCase pushCardDeckUseCase;
    private final PushSessionToFoundryUseCase pushSessionUseCase;
    private final PushCategoryToFoundryUseCase pushCategoryUseCase;
    private final PushWorldWikiToFoundryUseCase pushWorldWikiUseCase;
    private final PushCampaignToFoundryUseCase pushCampaignUseCase;
    private final GetFoundryPushStatusUseCase statusUseCase;
    private final FoundryPushWebMapper mapper;

    public FoundryPushController(PushArticleToFoundryUseCase pushArticleUseCase,
                                 PushHandoutToFoundryUseCase pushHandoutUseCase,
                                 PushRollTableToFoundryUseCase pushRollTableUseCase,
                                 PushCardDeckToFoundryUseCase pushCardDeckUseCase,
                                 PushSessionToFoundryUseCase pushSessionUseCase,
                                 PushCategoryToFoundryUseCase pushCategoryUseCase,
                                 PushWorldWikiToFoundryUseCase pushWorldWikiUseCase,
                                 PushCampaignToFoundryUseCase pushCampaignUseCase,
                                 GetFoundryPushStatusUseCase statusUseCase, FoundryPushWebMapper mapper) {
        this.pushArticleUseCase = pushArticleUseCase;
        this.pushHandoutUseCase = pushHandoutUseCase;
        this.pushRollTableUseCase = pushRollTableUseCase;
        this.pushCardDeckUseCase = pushCardDeckUseCase;
        this.pushSessionUseCase = pushSessionUseCase;
        this.pushCategoryUseCase = pushCategoryUseCase;
        this.pushWorldWikiUseCase = pushWorldWikiUseCase;
        this.pushCampaignUseCase = pushCampaignUseCase;
        this.statusUseCase = statusUseCase;
        this.mapper = mapper;
    }

    @PostMapping("/articles/{articleId}/push")
    @ResponseStatus(HttpStatus.OK)
    public FoundryPushResponse pushArticle(@PathVariable UUID worldId, @PathVariable UUID articleId) {
        return mapper.toResponse(pushArticleUseCase.push(worldId, articleId));
    }

    @GetMapping("/articles/{articleId}/push-status")
    public FoundryPushStatusResponse articlePushStatus(@PathVariable UUID worldId, @PathVariable UUID articleId) {
        return mapper.toStatusResponse(statusUseCase.statusFor(worldId, FoundryEntityType.ARTICLE, articleId));
    }

    @PostMapping("/handouts/{handoutId}/push")
    @ResponseStatus(HttpStatus.OK)
    public FoundryPushResponse pushHandout(@PathVariable UUID worldId, @PathVariable UUID handoutId) {
        return mapper.toResponse(pushHandoutUseCase.pushHandout(worldId, handoutId));
    }

    @GetMapping("/handouts/{handoutId}/push-status")
    public FoundryPushStatusResponse handoutPushStatus(@PathVariable UUID worldId, @PathVariable UUID handoutId) {
        return mapper.toStatusResponse(statusUseCase.statusFor(worldId, FoundryEntityType.HANDOUT, handoutId));
    }

    @PostMapping("/roll-tables/{rollTableId}/push")
    @ResponseStatus(HttpStatus.OK)
    public FoundryPushResponse pushRollTable(@PathVariable UUID worldId, @PathVariable UUID rollTableId) {
        return mapper.toResponse(pushRollTableUseCase.pushRollTable(worldId, rollTableId));
    }

    @GetMapping("/roll-tables/{rollTableId}/push-status")
    public FoundryPushStatusResponse rollTablePushStatus(@PathVariable UUID worldId,
                                                         @PathVariable UUID rollTableId) {
        return mapper.toStatusResponse(statusUseCase.statusFor(worldId, FoundryEntityType.ROLL_TABLE, rollTableId));
    }

    @PostMapping("/card-decks/{cardDeckId}/push")
    @ResponseStatus(HttpStatus.OK)
    public FoundryPushResponse pushCardDeck(@PathVariable UUID worldId, @PathVariable UUID cardDeckId) {
        return mapper.toResponse(pushCardDeckUseCase.pushCardDeck(worldId, cardDeckId));
    }

    @GetMapping("/card-decks/{cardDeckId}/push-status")
    public FoundryPushStatusResponse cardDeckPushStatus(@PathVariable UUID worldId,
                                                        @PathVariable UUID cardDeckId) {
        return mapper.toStatusResponse(statusUseCase.statusFor(worldId, FoundryEntityType.CARD_DECK, cardDeckId));
    }

    @PostMapping("/campaigns/{campaignId}/sessions/{sessionId}/push")
    @ResponseStatus(HttpStatus.OK)
    public FoundrySessionPushResponse pushSession(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                                  @PathVariable UUID sessionId) {
        return mapper.toSessionResponse(pushSessionUseCase.pushSession(worldId, campaignId, sessionId));
    }

    @PostMapping("/campaigns/{campaignId}/push")
    @ResponseStatus(HttpStatus.OK)
    public FoundryCampaignPushResponse pushCampaign(@PathVariable UUID worldId, @PathVariable UUID campaignId) {
        return mapper.toCampaignResponse(pushCampaignUseCase.pushCampaign(worldId, campaignId));
    }

    @PostMapping("/categories/{categoryId}/push")
    @ResponseStatus(HttpStatus.OK)
    public FoundryCategoryPushResponse pushCategory(@PathVariable UUID worldId, @PathVariable UUID categoryId,
                                                    @RequestParam FoundryCategoryPushMode mode) {
        return mapper.toCategoryResponse(pushCategoryUseCase.pushCategory(worldId, categoryId, mode));
    }

    @GetMapping("/categories/{categoryId}/push-status")
    public FoundryPushStatusResponse categoryPushStatus(@PathVariable UUID worldId, @PathVariable UUID categoryId) {
        return mapper.toStatusResponse(statusUseCase.statusFor(worldId, FoundryEntityType.CATEGORY, categoryId));
    }

    @PostMapping("/wiki/push")
    @ResponseStatus(HttpStatus.OK)
    public FoundryCategoryPushResponse pushWorldWiki(@PathVariable UUID worldId) {
        return mapper.toCategoryResponse(pushWorldWikiUseCase.pushWorldWiki(worldId));
    }

    @GetMapping("/wiki/push-status")
    public FoundryPushStatusResponse worldWikiPushStatus(@PathVariable UUID worldId) {
        return mapper.toStatusResponse(statusUseCase.statusFor(worldId, FoundryEntityType.WIKI, worldId));
    }
}
