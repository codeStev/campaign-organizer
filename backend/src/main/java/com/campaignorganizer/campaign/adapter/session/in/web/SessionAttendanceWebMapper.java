package com.campaignorganizer.campaign.adapter.session.in.web;

import com.campaignorganizer.campaign.adapter.session.in.web.SessionAttendanceWebDtos.AttendanceEntryRequest;
import com.campaignorganizer.campaign.adapter.session.in.web.SessionAttendanceWebDtos.AttendanceEntryResponse;
import com.campaignorganizer.campaign.adapter.session.in.web.SessionAttendanceWebDtos.AttendanceRequest;
import com.campaignorganizer.campaign.application.session.port.in.AttendanceCommands;
import com.campaignorganizer.campaign.application.session.port.in.AttendanceCommands.AttendanceEntryInput;
import com.campaignorganizer.campaign.application.session.port.in.AttendanceEntry;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SessionAttendanceWebMapper {

    AttendanceEntryResponse toResponse(AttendanceEntry entry);

    List<AttendanceEntryResponse> toResponses(List<AttendanceEntry> entries);

    AttendanceEntryInput toInput(AttendanceEntryRequest request);

    List<AttendanceEntryInput> toInputs(List<AttendanceEntryRequest> requests);

    default AttendanceCommands.PutSessionAttendanceCommand toPutCommand(UUID worldId, UUID campaignId,
                                                                        UUID sessionId,
                                                                        AttendanceRequest request) {
        return new AttendanceCommands.PutSessionAttendanceCommand(worldId, campaignId, sessionId,
                toInputs(request.entries()));
    }
}
