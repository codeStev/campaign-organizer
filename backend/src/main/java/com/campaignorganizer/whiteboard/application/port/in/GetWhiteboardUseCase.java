package com.campaignorganizer.whiteboard.application.port.in;

import com.campaignorganizer.whiteboard.application.port.published.WhiteboardView;
import java.util.UUID;

public interface GetWhiteboardUseCase {

    WhiteboardView get(UUID worldId, UUID whiteboardId);
}
