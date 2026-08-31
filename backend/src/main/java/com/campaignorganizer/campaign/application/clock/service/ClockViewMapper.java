package com.campaignorganizer.campaign.application.clock.service;

import com.campaignorganizer.campaign.application.clock.port.published.ClockSegmentView;
import com.campaignorganizer.campaign.application.clock.port.published.ClockView;
import com.campaignorganizer.campaign.domain.clock.ClockSegment;
import com.campaignorganizer.campaign.domain.clock.GameClock;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClockViewMapper {

    ClockView toView(GameClock clock);

    ClockSegmentView toSegmentView(ClockSegment segment);
}
