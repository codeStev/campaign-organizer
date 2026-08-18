package com.campaignorganizer.campaign.application.campaign.port.out;

import java.util.UUID;

public interface WorldExistsPort {

    boolean exists(UUID worldId);
}
