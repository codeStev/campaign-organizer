package com.campaignorganizer.campaign.application.campaign.port.out;

import java.util.UUID;

public interface GameSystemExistsPort {

    boolean exists(UUID systemId);
}
