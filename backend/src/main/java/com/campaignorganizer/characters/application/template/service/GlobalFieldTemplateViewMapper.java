package com.campaignorganizer.characters.application.template.service;

import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateView;
import com.campaignorganizer.characters.domain.template.GlobalFieldTemplate;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GlobalFieldTemplateViewMapper {

    GlobalFieldTemplateView toView(GlobalFieldTemplate template);
}
