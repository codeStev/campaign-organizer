package com.campaignorganizer.campaign.application.arc.service;

import com.campaignorganizer.campaign.application.arc.port.published.ArcView;
import com.campaignorganizer.campaign.domain.arc.Arc;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArcViewMapper {

    ArcView toView(Arc arc);
}
