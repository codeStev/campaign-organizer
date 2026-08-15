package com.campaignorganizer.media.adapter.in.web;

import com.campaignorganizer.media.application.port.in.MediaView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps the application view to the web response, deriving the content URL (MapStruct). */
@Mapper(componentModel = "spring")
public interface MediaWebMapper {

    @Mapping(target = "size", source = "sizeBytes")
    @Mapping(target = "url", expression = "java(\"/api/media/\" + view.id() + \"/content\")")
    MediaResponse toResponse(MediaView view);
}
