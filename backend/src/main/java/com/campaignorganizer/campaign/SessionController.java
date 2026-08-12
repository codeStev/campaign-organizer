package com.campaignorganizer.campaign;

import com.campaignorganizer.campaign.SessionDtos.SessionRequest;
import com.campaignorganizer.campaign.SessionDtos.SessionResponse;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/worlds/{worldId}/campaigns/{campaignId}/sessions")
public class SessionController {

    private final SessionRepository sessions;
    private final CampaignRepository campaigns;

    public SessionController(SessionRepository sessions, CampaignRepository campaigns) {
        this.sessions = sessions;
        this.campaigns = campaigns;
    }

    @GetMapping
    public List<SessionResponse> list(@PathVariable UUID worldId, @PathVariable UUID campaignId) {
        requireCampaign(worldId, campaignId);
        return sessions.findOrdered(campaignId).stream()
                .map(SessionResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<SessionResponse> create(@PathVariable UUID worldId,
                                                  @PathVariable UUID campaignId,
                                                  @Valid @RequestBody SessionRequest request) {
        requireCampaign(worldId, campaignId);
        Session saved = sessions.save(new Session(campaignId, request.title(),
                request.sessionNumber(), request.date(), request.summary(), request.notes()));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/campaigns/" + campaignId
                        + "/sessions/" + saved.getId()))
                .body(SessionResponse.from(saved));
    }

    @PutMapping("/{sessionId}")
    public SessionResponse update(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                  @PathVariable UUID sessionId, @Valid @RequestBody SessionRequest request) {
        requireCampaign(worldId, campaignId);
        Session session = sessions.findByIdAndCampaignId(sessionId, campaignId)
                .orElseThrow(this::sessionNotFound);
        session.update(request.title(), request.sessionNumber(), request.date(),
                request.summary(), request.notes());
        return SessionResponse.from(sessions.save(session));
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                       @PathVariable UUID sessionId) {
        requireCampaign(worldId, campaignId);
        Session session = sessions.findByIdAndCampaignId(sessionId, campaignId)
                .orElseThrow(this::sessionNotFound);
        sessions.delete(session);
    }

    private void requireCampaign(UUID worldId, UUID campaignId) {
        if (!campaigns.existsByIdAndWorldId(campaignId, worldId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found");
        }
    }

    private ResponseStatusException sessionNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
    }
}
