package com.campaignorganizer.whiteboard.application.port.in;

import com.campaignorganizer.whiteboard.application.port.in.WhiteboardCommands.CreateWhiteboardCommand;
import com.campaignorganizer.whiteboard.application.port.published.WhiteboardView;

public interface CreateWhiteboardUseCase {

    WhiteboardView create(CreateWhiteboardCommand command);
}
