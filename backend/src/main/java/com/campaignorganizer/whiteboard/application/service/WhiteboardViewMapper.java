package com.campaignorganizer.whiteboard.application.service;

import com.campaignorganizer.whiteboard.application.port.in.WhiteboardView;
import com.campaignorganizer.whiteboard.domain.Whiteboard;
import org.mapstruct.Mapper;

/** Maps the domain aggregate to the application read model (MapStruct). */
@Mapper(componentModel = "spring")
public interface WhiteboardViewMapper {

    WhiteboardView toView(Whiteboard whiteboard);
}
