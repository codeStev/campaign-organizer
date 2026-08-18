package com.campaignorganizer.media.application.port.published;

import java.util.UUID;

/**
 * Published port: lets other bounded contexts (e.g. worldbuilding maps) check that
 * a media asset exists in a world, without touching this context's internals.
 */
public interface MediaLookupPort {

    boolean existsInWorld(UUID mediaId, UUID worldId);
}
