package com.campaignorganizer.media.application.port.in;

import java.util.List;
import java.util.UUID;

public interface ListMediaUseCase {

    List<MediaView> list(UUID worldId);
}
