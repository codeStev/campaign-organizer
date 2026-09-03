package com.campaignorganizer.campaign.application.beatkind.port.out;

import java.util.UUID;

public interface WorldExistsPort {

    boolean exists(UUID worldId);
}
