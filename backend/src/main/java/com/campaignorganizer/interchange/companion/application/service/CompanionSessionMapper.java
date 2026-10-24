package com.campaignorganizer.interchange.companion.application.service;

import com.campaignorganizer.campaign.application.session.port.published.SessionExportView;
import com.campaignorganizer.campaign.application.session.port.published.SessionTranscriptView;
import com.campaignorganizer.interchange.companion.application.port.in.CompanionSession;
import com.campaignorganizer.interchange.companion.application.port.in.ReplaceSessionTranscriptUseCase.TranscriptReceipt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompanionSessionMapper {

    CompanionSession toSession(SessionExportView view);

    @Mapping(target = "lineCount", expression = "java(view.lines().size())")
    TranscriptReceipt toReceipt(SessionTranscriptView view);
}
