package com.campaignorganizer.whiteboard.application.port.in;

import com.campaignorganizer.whiteboard.application.port.in.WhiteboardCommands.UpdateWhiteboardCommand;

public interface UpdateWhiteboardUseCase {

    WhiteboardView update(UpdateWhiteboardCommand command);
}
