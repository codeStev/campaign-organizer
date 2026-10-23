package com.campaignorganizer.campaign.adapter.session.in.web;

import com.campaignorganizer.campaign.adapter.session.in.web.SessionTranscriptWebDtos.SessionTranscriptResponse;
import com.campaignorganizer.campaign.application.session.port.in.DeleteSessionTranscriptUseCase;
import com.campaignorganizer.campaign.application.session.port.in.GetSessionTranscriptUseCase;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Thin web adapter for the GM's view of a session transcript (ADR-0121); written by the companion export. */
@RestController
@RequestMapping("/api/worlds/{worldId}/campaigns/{campaignId}/sessions/{sessionId}/transcript")
public class SessionTranscriptController {

    private final GetSessionTranscriptUseCase getUseCase;
    private final DeleteSessionTranscriptUseCase deleteUseCase;
    private final SessionTranscriptWebMapper mapper;

    public SessionTranscriptController(GetSessionTranscriptUseCase getUseCase,
                                       DeleteSessionTranscriptUseCase deleteUseCase,
                                       SessionTranscriptWebMapper mapper) {
        this.getUseCase = getUseCase;
        this.deleteUseCase = deleteUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public SessionTranscriptResponse get(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                         @PathVariable UUID sessionId) {
        return mapper.toResponse(getUseCase.get(worldId, campaignId, sessionId));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID campaignId, @PathVariable UUID sessionId) {
        deleteUseCase.delete(worldId, campaignId, sessionId);
    }
}
