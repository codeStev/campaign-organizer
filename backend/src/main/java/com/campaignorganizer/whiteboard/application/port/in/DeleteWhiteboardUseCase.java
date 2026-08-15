package com.campaignorganizer.whiteboard.application.port.in;

import java.util.UUID;

public interface DeleteWhiteboardUseCase {

    void delete(UUID worldId, UUID whiteboardId);
}
