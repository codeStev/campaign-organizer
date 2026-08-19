package com.campaignorganizer.interchange.packet.application.port.in;

import com.campaignorganizer.interchange.packet.application.port.in.SessionPacketDtos.SessionPacketResponse;
import java.util.UUID;

public interface BuildSessionPacketUseCase {

    SessionPacketResponse packet(UUID worldId, UUID campaignId, UUID sessionId);
}
