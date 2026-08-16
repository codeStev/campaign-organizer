package com.campaignorganizer.worldbuilding.application.map.port.published;

import java.util.List;
import java.util.UUID;

/** Published port: read map pins from other contexts (usage, export, packet). */
public interface MapPinQueryPort {

    List<MapPinView> findByMap(UUID mapId);

    List<MapPinView> findByArticle(UUID articleId);
}
