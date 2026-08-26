package com.campaignorganizer.handouts.application.service;

import com.campaignorganizer.handouts.application.port.published.HandoutView;
import com.campaignorganizer.handouts.domain.Handout;
import org.mapstruct.Mapper;

/** Maps the domain handout to the published read model (MapStruct). */
@Mapper(componentModel = "spring")
public interface HandoutViewMapper {

    HandoutView toView(Handout handout);
}
