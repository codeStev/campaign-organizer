package com.campaignorganizer.whiteboard.application.port.in;

import com.campaignorganizer.whiteboard.application.port.in.WhiteboardCommands.CreateWhiteboardCommand;

public interface CreateWhiteboardUseCase {

    WhiteboardView create(CreateWhiteboardCommand command);
}
