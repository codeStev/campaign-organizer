package com.campaignorganizer.interchange.packet.adapter.in.web;

import com.campaignorganizer.interchange.packet.application.port.in.BuildSessionPacketUseCase;
import com.campaignorganizer.interchange.packet.application.port.in.SessionPacketDtos.SessionPacketResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/worlds/{worldId}/campaigns/{campaignId}/sessions/{sessionId}/packet")
public class SessionPacketController {

    private final BuildSessionPacketUseCase packets;

    public SessionPacketController(BuildSessionPacketUseCase packets) {
        this.packets = packets;
    }

    @GetMapping
    public SessionPacketResponse get(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                     @PathVariable UUID sessionId) {
        return packets.packet(worldId, campaignId, sessionId);
    }
}
