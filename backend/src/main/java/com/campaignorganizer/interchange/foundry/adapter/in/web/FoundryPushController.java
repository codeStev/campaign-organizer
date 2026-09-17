package com.campaignorganizer.interchange.foundry.adapter.in.web;

import com.campaignorganizer.interchange.foundry.adapter.in.web.FoundryPushWebDtos.FoundryPushResponse;
import com.campaignorganizer.interchange.foundry.adapter.in.web.FoundryPushWebDtos.FoundryPushStatusResponse;
import com.campaignorganizer.interchange.foundry.application.port.in.GetFoundryPushStatusUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.PushArticleToFoundryUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.PushHandoutToFoundryUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.PushRollTableToFoundryUseCase;
import com.campaignorganizer.interchange.foundry.domain.FoundryEntityType;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Push Campaign Organizer content to Foundry VTT (ADR-0115). */
@RestController
@RequestMapping("/api/worlds/{worldId}/foundry")
public class FoundryPushController {

    private final PushArticleToFoundryUseCase pushArticleUseCase;
    private final PushHandoutToFoundryUseCase pushHandoutUseCase;
    private final PushRollTableToFoundryUseCase pushRollTableUseCase;
    private final GetFoundryPushStatusUseCase statusUseCase;
    private final FoundryPushWebMapper mapper;

    public FoundryPushController(PushArticleToFoundryUseCase pushArticleUseCase,
                                 PushHandoutToFoundryUseCase pushHandoutUseCase,
                                 PushRollTableToFoundryUseCase pushRollTableUseCase,
                                 GetFoundryPushStatusUseCase statusUseCase, FoundryPushWebMapper mapper) {
        this.pushArticleUseCase = pushArticleUseCase;
        this.pushHandoutUseCase = pushHandoutUseCase;
        this.pushRollTableUseCase = pushRollTableUseCase;
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
}
