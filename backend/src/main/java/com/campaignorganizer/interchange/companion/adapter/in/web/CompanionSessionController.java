package com.campaignorganizer.interchange.companion.adapter.in.web;

import com.campaignorganizer.interchange.companion.adapter.in.web.CompanionWebDtos.CompanionSessionResponse;
import com.campaignorganizer.interchange.companion.adapter.in.web.CompanionWebDtos.CreateCompanionSessionRequest;
import com.campaignorganizer.interchange.companion.adapter.in.web.CompanionWebDtos.ReplaceTranscriptRequest;
import com.campaignorganizer.interchange.companion.adapter.in.web.CompanionWebDtos.TranscriptReceiptResponse;
import com.campaignorganizer.interchange.companion.adapter.in.web.CompanionWebDtos.WriteSessionSummaryRequest;
import com.campaignorganizer.interchange.companion.application.port.in.CreateCompanionSessionUseCase;
import com.campaignorganizer.interchange.companion.application.port.in.DeleteSessionTranscriptUseCase;
import com.campaignorganizer.interchange.companion.application.port.in.ListCompanionSessionsUseCase;
import com.campaignorganizer.interchange.companion.application.port.in.ReplaceSessionTranscriptUseCase;
import com.campaignorganizer.interchange.companion.application.port.in.WriteSessionSummaryUseCase;
import com.campaignorganizer.security.CurrentUserPort;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Session, summary and transcript export for companion apps such as DiscordWhispr (ADR-0121). */
@RestController
@RequestMapping("/api/companion/campaigns/{campaignId}/sessions")
public class CompanionSessionController {

    private final ListCompanionSessionsUseCase listSessions;
    private final CreateCompanionSessionUseCase createSession;
    private final WriteSessionSummaryUseCase writeSummary;
    private final ReplaceSessionTranscriptUseCase replaceTranscript;
    private final DeleteSessionTranscriptUseCase deleteTranscript;
    private final CurrentUserPort currentUser;
    private final CompanionSessionWebMapper mapper;

    public CompanionSessionController(ListCompanionSessionsUseCase listSessions,
                                      CreateCompanionSessionUseCase createSession,
                                      WriteSessionSummaryUseCase writeSummary,
                                      ReplaceSessionTranscriptUseCase replaceTranscript,
                                      DeleteSessionTranscriptUseCase deleteTranscript, CurrentUserPort currentUser,
                                      CompanionSessionWebMapper mapper) {
        this.listSessions = listSessions;
        this.createSession = createSession;
        this.writeSummary = writeSummary;
        this.replaceTranscript = replaceTranscript;
        this.deleteTranscript = deleteTranscript;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @GetMapping
    public List<CompanionSessionResponse> list(@PathVariable UUID campaignId) {
        return listSessions.list(currentUser.currentAccountId(), campaignId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanionSessionResponse create(@PathVariable UUID campaignId,
                                           @Valid @RequestBody CreateCompanionSessionRequest request) {
        return mapper.toResponse(createSession.create(mapper.toCommand(currentUser.currentAccountId(), campaignId,
                request)));
    }

    @PutMapping("/{sessionId}/summary")
    public CompanionSessionResponse summary(@PathVariable UUID campaignId, @PathVariable UUID sessionId,
                                            @Valid @RequestBody WriteSessionSummaryRequest request) {
        return mapper.toResponse(writeSummary.write(mapper.toCommand(currentUser.currentAccountId(), campaignId,
                sessionId, request)));
    }

    @PutMapping("/{sessionId}/transcript")
    public TranscriptReceiptResponse replaceTranscript(@PathVariable UUID campaignId, @PathVariable UUID sessionId,
                                                       @Valid @RequestBody ReplaceTranscriptRequest request) {
        return mapper.toResponse(replaceTranscript.replace(mapper.toCommand(currentUser.currentAccountId(), campaignId,
                sessionId, request)));
    }

    @DeleteMapping("/{sessionId}/transcript")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTranscript(@PathVariable UUID campaignId, @PathVariable UUID sessionId) {
        deleteTranscript.delete(currentUser.currentAccountId(), campaignId, sessionId);
    }
}
