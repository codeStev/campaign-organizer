package com.campaignorganizer.campaign.application.beatkind.service;

import com.campaignorganizer.campaign.application.beatkind.port.published.BeatKindView;
import com.campaignorganizer.campaign.domain.beatkind.BeatKind;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BeatKindViewMapper {

    BeatKindView toView(BeatKind beatKind);
}
