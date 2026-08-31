package com.campaignorganizer.campaign.application.loosethread.service;

import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadView;
import com.campaignorganizer.campaign.domain.loosethread.LooseThread;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LooseThreadViewMapper {

    LooseThreadView toView(LooseThread thread);
}
