package com.campaignorganizer.campaign.application.session.service;

import com.campaignorganizer.campaign.application.session.port.published.SessionTranscriptView;
import com.campaignorganizer.campaign.domain.session.SessionTranscript;
import com.campaignorganizer.campaign.domain.session.TranscriptLine;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SessionTranscriptViewMapper {

    SessionTranscriptView toView(SessionTranscript transcript);

    default SessionTranscriptView.LineView toLineView(TranscriptLine line) {
        return new SessionTranscriptView.LineView(line.offsetMs(), line.speaker(), line.kind().name(), line.text());
    }
}
