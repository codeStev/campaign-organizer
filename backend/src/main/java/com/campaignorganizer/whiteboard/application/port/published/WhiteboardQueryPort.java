package com.campaignorganizer.whiteboard.application.port.published;

import java.util.List;
import java.util.UUID;

/** Published port: read whiteboards from other contexts (export). */
public interface WhiteboardQueryPort {

    List<WhiteboardView> findByWorld(UUID worldId);
}
