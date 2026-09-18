package com.campaignorganizer.worldbuilding.application.wiki.port.in;

import com.campaignorganizer.worldbuilding.application.wiki.port.in.AutolinkDtos.AutolinkSelection;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import java.util.List;
import java.util.UUID;

public interface ApplyAutolinksUseCase {

    /** Re-scans the article's current body and converts only the selected
     * occurrences into real [[links]] (ADR-0116), through the normal article
     * update path (so this creates a revision like any other edit). */
    ArticleView apply(UUID worldId, UUID articleId, List<AutolinkSelection> selections);
}
