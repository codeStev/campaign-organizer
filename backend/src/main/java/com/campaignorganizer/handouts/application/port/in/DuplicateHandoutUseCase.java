package com.campaignorganizer.handouts.application.port.in;

import com.campaignorganizer.handouts.application.port.published.HandoutView;
import java.util.UUID;

public interface DuplicateHandoutUseCase {

    HandoutView duplicate(UUID worldId, UUID handoutId);
}
