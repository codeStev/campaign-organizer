package com.campaignorganizer.campaign.adapter.session.in.web;

import com.campaignorganizer.campaign.adapter.session.in.web.CheatSheetWebDtos.CheatSheetRequest;
import com.campaignorganizer.campaign.adapter.session.in.web.CheatSheetWebDtos.CheatSheetResponse;
import com.campaignorganizer.campaign.application.session.port.in.DeleteCheatSheetUseCase;
import com.campaignorganizer.campaign.application.session.port.in.GetCheatSheetUseCase;
import com.campaignorganizer.campaign.application.session.port.in.PutCheatSheetUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Thin web adapter for the per-session cheat sheet (FR-37). */
@RestController
@RequestMapping("/api/worlds/{worldId}/campaigns/{campaignId}/sessions/{sessionId}/cheat-sheet")
public class CheatSheetController {

    private final GetCheatSheetUseCase getUseCase;
    private final PutCheatSheetUseCase putUseCase;
    private final DeleteCheatSheetUseCase deleteUseCase;
    private final CheatSheetWebMapper mapper;

    public CheatSheetController(GetCheatSheetUseCase getUseCase, PutCheatSheetUseCase putUseCase,
                                DeleteCheatSheetUseCase deleteUseCase, CheatSheetWebMapper mapper) {
        this.getUseCase = getUseCase;
        this.putUseCase = putUseCase;
        this.deleteUseCase = deleteUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public CheatSheetResponse get(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                  @PathVariable UUID sessionId) {
        return mapper.toResponse(getUseCase.get(worldId, campaignId, sessionId));
    }

    @PutMapping
    public CheatSheetResponse put(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                  @PathVariable UUID sessionId,
                                  @Valid @RequestBody CheatSheetRequest request) {
        return mapper.toResponse(putUseCase.put(mapper.toPutCommand(worldId, campaignId,
                sessionId, request)));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                       @PathVariable UUID sessionId) {
        deleteUseCase.delete(worldId, campaignId, sessionId);
    }
}
