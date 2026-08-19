package com.campaignorganizer.whiteboard.application.port.in;

import com.campaignorganizer.whiteboard.application.port.in.WhiteboardCommands.UpdateWhiteboardCommand;
import com.campaignorganizer.whiteboard.application.port.published.WhiteboardView;

public interface UpdateWhiteboardUseCase {

    WhiteboardView update(UpdateWhiteboardCommand command);
}
