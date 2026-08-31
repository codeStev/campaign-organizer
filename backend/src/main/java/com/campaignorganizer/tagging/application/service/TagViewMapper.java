package com.campaignorganizer.tagging.application.service;

import com.campaignorganizer.tagging.application.port.published.TagView;
import com.campaignorganizer.tagging.domain.EntityTag;
import org.mapstruct.Mapper;

/** Maps the domain tag to the published read model (MapStruct). */
@Mapper(componentModel = "spring")
public interface TagViewMapper {

    TagView toView(EntityTag tag);
}
