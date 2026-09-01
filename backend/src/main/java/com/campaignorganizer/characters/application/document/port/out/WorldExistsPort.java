package com.campaignorganizer.characters.application.document.port.out;

import java.util.UUID;

public interface WorldExistsPort {

    boolean exists(UUID worldId);
}
