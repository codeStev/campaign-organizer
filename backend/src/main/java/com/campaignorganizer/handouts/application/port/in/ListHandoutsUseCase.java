package com.campaignorganizer.handouts.application.port.in;

import com.campaignorganizer.handouts.application.port.published.HandoutView;
import java.util.List;
import java.util.UUID;

public interface ListHandoutsUseCase {

    List<HandoutView> list(UUID worldId);
}
