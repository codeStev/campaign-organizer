package com.campaignorganizer.interchange.companion.adapter.in.web;

import com.campaignorganizer.interchange.companion.adapter.in.web.CompanionWebDtos.CompanionSessionResponse;
import com.campaignorganizer.interchange.companion.adapter.in.web.CompanionWebDtos.CreateCompanionSessionRequest;
import com.campaignorganizer.interchange.companion.adapter.in.web.CompanionWebDtos.ReplaceTranscriptRequest;
import com.campaignorganizer.interchange.companion.adapter.in.web.CompanionWebDtos.TranscriptReceiptResponse;
import com.campaignorganizer.interchange.companion.adapter.in.web.CompanionWebDtos.WriteSessionSummaryRequest;
import com.campaignorganizer.interchange.companion.application.port.in.CompanionSession;
import com.campaignorganizer.interchange.companion.application.port.in.CreateCompanionSessionUseCase.CreateCompanionSessionCommand;
import com.campaignorganizer.interchange.companion.application.port.in.ReplaceSessionTranscriptUseCase.ReplaceSessionTranscriptCommand;
import com.campaignorganizer.interchange.companion.application.port.in.ReplaceSessionTranscriptUseCase.TranscriptReceipt;
import com.campaignorganizer.interchange.companion.application.port.in.WriteSessionSummaryUseCase.WriteSessionSummaryCommand;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompanionSessionWebMapper {

    CompanionSessionResponse toResponse(CompanionSession session);

    TranscriptReceiptResponse toResponse(TranscriptReceipt receipt);

    CreateCompanionSessionCommand toCommand(UUID accountId, UUID campaignId, CreateCompanionSessionRequest request);

    WriteSessionSummaryCommand toCommand(UUID accountId, UUID campaignId, UUID sessionId,
                                         WriteSessionSummaryRequest request);

    ReplaceSessionTranscriptCommand toCommand(UUID accountId, UUID campaignId, UUID sessionId,
                                              ReplaceTranscriptRequest request);
}
