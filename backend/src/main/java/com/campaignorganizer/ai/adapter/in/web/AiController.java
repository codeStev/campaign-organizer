package com.campaignorganizer.ai.adapter.in.web;

import com.campaignorganizer.ai.application.port.in.DraftArticleTextUseCase;
import com.campaignorganizer.ai.application.port.in.DraftArticleTextUseCase.DraftArticleTextCommand;
import com.campaignorganizer.ai.application.port.in.SummarizeSessionNotesUseCase;
import com.campaignorganizer.ai.application.port.in.SummarizeSessionNotesUseCase.SummarizeSessionNotesCommand;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin web adapter for AI text drafting (ADR-0064). {@code worldId} is accepted
 * only for URL-shape consistency with every other world-scoped endpoint (and so
 * the frontend's {@code xApi(worldId)} client-factory convention keeps working)
 * - this call is stateless and never reads or writes world data.
 */
@RestController
@RequestMapping("/api/worlds/{worldId}/ai")
public class AiController {

    private final DraftArticleTextUseCase draftUseCase;
    private final SummarizeSessionNotesUseCase summarizeUseCase;
    private final AiWebMapper mapper;

    public AiController(
            DraftArticleTextUseCase draftUseCase, SummarizeSessionNotesUseCase summarizeUseCase, AiWebMapper mapper) {
        this.draftUseCase = draftUseCase;
        this.summarizeUseCase = summarizeUseCase;
        this.mapper = mapper;
    }

    @PostMapping("/draft-article-text")
    public DraftArticleTextResponse draftArticleText(
            @PathVariable UUID worldId, @Valid @RequestBody DraftArticleTextRequest request) {
        var command = new DraftArticleTextCommand(
                request.instructions(), request.existingContent(), request.level(), request.template());
        return mapper.toResponse(draftUseCase.draft(command));
    }

    @PostMapping("/summarize-session-notes")
    public SummarizeSessionNotesResponse summarizeSessionNotes(
            @PathVariable UUID worldId, @Valid @RequestBody SummarizeSessionNotesRequest request) {
        var command = new SummarizeSessionNotesCommand(request.notes());
        return mapper.toSummarizeResponse(summarizeUseCase.summarize(command));
    }
}
