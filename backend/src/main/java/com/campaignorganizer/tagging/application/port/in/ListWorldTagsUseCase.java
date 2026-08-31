package com.campaignorganizer.tagging.application.port.in;

import java.util.List;
import java.util.UUID;

public interface ListWorldTagsUseCase {

    /** Every distinct tag name used anywhere in the world, alphabetical (autocomplete source). */
    List<String> list(UUID worldId);
}
