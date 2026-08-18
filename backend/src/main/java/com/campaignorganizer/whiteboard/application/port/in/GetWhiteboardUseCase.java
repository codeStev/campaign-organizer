package com.campaignorganizer.whiteboard.application.port.in;

import java.util.UUID;

public interface GetWhiteboardUseCase {

    WhiteboardView get(UUID worldId, UUID whiteboardId);
}
