package com.campaignorganizer.media.application.service;

import com.campaignorganizer.media.application.port.in.MediaView;
import com.campaignorganizer.media.domain.MediaAsset;
import org.mapstruct.Mapper;

/** Maps the domain asset to the application read model (MapStruct). */
@Mapper(componentModel = "spring")
public interface MediaViewMapper {

    MediaView toView(MediaAsset asset);
}
