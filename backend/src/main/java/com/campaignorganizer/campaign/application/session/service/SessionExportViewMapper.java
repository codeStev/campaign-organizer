package com.campaignorganizer.campaign.application.session.service;

import com.campaignorganizer.campaign.application.session.port.published.SessionExportView;
import com.campaignorganizer.campaign.domain.session.Session;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SessionExportViewMapper {

    @Mapping(target = "hasSummary", expression = "java(session.hasSummary())")
    SessionExportView toView(Session session);
}
