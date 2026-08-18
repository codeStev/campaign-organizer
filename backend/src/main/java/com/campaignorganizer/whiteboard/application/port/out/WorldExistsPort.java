package com.campaignorganizer.whiteboard.application.port.out;

import java.util.UUID;

/** Outbound port to check a world exists before attaching a whiteboard to it. */
public interface WorldExistsPort {

    boolean exists(UUID worldId);
}
