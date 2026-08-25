package com.campaignorganizer.campaign.application.arc.service;

import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatView;
import com.campaignorganizer.campaign.domain.arc.ArcBeat;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArcBeatViewMapper {

    default ArcBeatView toView(ArcBeat beat) {
        if (beat == null) {
            return null;
        }
        return new ArcBeatView(beat.getId(), beat.getArcId(), beat.getTitle(), beat.getBody(),
                beat.isDone(), List.copyOf(beat.getArticleIds()), List.copyOf(beat.getStatblockIds()),
                List.copyOf(beat.getTableIds()), List.copyOf(beat.getDeckIds()),
                beat.getSessionId(), beat.getPosition(), beat.getCreatedAt(), beat.getUpdatedAt());
    }
}
