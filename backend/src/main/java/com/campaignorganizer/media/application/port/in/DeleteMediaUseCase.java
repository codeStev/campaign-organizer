package com.campaignorganizer.media.application.port.in;

import java.util.UUID;

public interface DeleteMediaUseCase {

    void delete(UUID worldId, UUID mediaId);
}
