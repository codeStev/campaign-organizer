package com.campaignorganizer.interchange.tags.application.port.in;

import com.campaignorganizer.interchange.tags.application.port.in.TagBrowseDtos.TagBrowseResult;
import java.util.UUID;

/** ADR-0083: everything across the world tagged with a given tag. */
public interface BrowseTagUseCase {

    TagBrowseResult browse(UUID worldId, String tag);
}
