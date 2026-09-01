package com.campaignorganizer.campaign.adapter.session.in.web;

import com.campaignorganizer.campaign.adapter.session.in.web.SessionAttendanceWebDtos.AttendanceEntryResponse;
import com.campaignorganizer.campaign.adapter.session.in.web.SessionAttendanceWebDtos.AttendanceRequest;
import com.campaignorganizer.campaign.application.session.port.in.GetSessionAttendanceUseCase;
import com.campaignorganizer.campaign.application.session.port.in.PutSessionAttendanceUseCase;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Thin web adapter for per-session attendance (ADR-0091). */
@RestController
@RequestMapping("/api/worlds/{worldId}/campaigns/{campaignId}/sessions/{sessionId}/attendance")
public class SessionAttendanceController {

    private final GetSessionAttendanceUseCase getUseCase;
    private final PutSessionAttendanceUseCase putUseCase;
    private final SessionAttendanceWebMapper mapper;

    public SessionAttendanceController(GetSessionAttendanceUseCase getUseCase,
                                       PutSessionAttendanceUseCase putUseCase,
                                       SessionAttendanceWebMapper mapper) {
        this.getUseCase = getUseCase;
        this.putUseCase = putUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<AttendanceEntryResponse> get(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                             @PathVariable UUID sessionId) {
        return mapper.toResponses(getUseCase.get(worldId, campaignId, sessionId));
    }

    @PutMapping
    public List<AttendanceEntryResponse> put(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                             @PathVariable UUID sessionId,
                                             @Valid @RequestBody AttendanceRequest request) {
        return mapper.toResponses(
                putUseCase.put(mapper.toPutCommand(worldId, campaignId, sessionId, request)));
    }
}
