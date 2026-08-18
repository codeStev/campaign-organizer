package com.campaignorganizer.campaign.application.session.service;

import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.campaign.domain.session.Session;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SessionViewMapper {

    SessionView toView(Session session);
}
