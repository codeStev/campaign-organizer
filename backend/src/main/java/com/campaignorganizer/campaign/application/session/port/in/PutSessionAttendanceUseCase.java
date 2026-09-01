package com.campaignorganizer.campaign.application.session.port.in;

import com.campaignorganizer.campaign.application.session.port.in.AttendanceCommands.PutSessionAttendanceCommand;
import java.util.List;

public interface PutSessionAttendanceUseCase {

    List<AttendanceEntry> put(PutSessionAttendanceCommand command);
}
