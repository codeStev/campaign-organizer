package com.campaignorganizer.worldbuilding.application.wiki.port.in;

import com.campaignorganizer.worldbuilding.application.wiki.port.in.AutolinkDtos.AutolinkCandidateGroup;
import java.util.List;
import java.util.UUID;

public interface ScanAutolinkCandidatesUseCase {

    /** One group per source article with at least one unlinked mention of another
     * article's title or alias (ADR-0116). */
    List<AutolinkCandidateGroup> scan(UUID worldId);
}
