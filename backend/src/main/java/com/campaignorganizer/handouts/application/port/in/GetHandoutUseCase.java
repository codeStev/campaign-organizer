package com.campaignorganizer.handouts.application.port.in;

import com.campaignorganizer.handouts.application.port.published.HandoutView;
import java.util.UUID;

public interface GetHandoutUseCase {

    HandoutView get(UUID worldId, UUID handoutId);
}
