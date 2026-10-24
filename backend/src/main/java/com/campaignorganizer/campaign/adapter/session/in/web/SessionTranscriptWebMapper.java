package com.campaignorganizer.campaign.adapter.session.in.web;

import com.campaignorganizer.campaign.adapter.session.in.web.SessionTranscriptWebDtos.SessionTranscriptResponse;
import com.campaignorganizer.campaign.adapter.session.in.web.SessionTranscriptWebDtos.TranscriptLineResponse;
import com.campaignorganizer.campaign.application.session.port.published.SessionTranscriptView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SessionTranscriptWebMapper {

    SessionTranscriptResponse toResponse(SessionTranscriptView view);

    TranscriptLineResponse toLineResponse(SessionTranscriptView.LineView view);
}
