package com.campaignorganizer.whiteboard.application.port.in;

import java.util.List;
import java.util.UUID;

public interface ListWhiteboardsUseCase {

    List<WhiteboardView> list(UUID worldId);
}
