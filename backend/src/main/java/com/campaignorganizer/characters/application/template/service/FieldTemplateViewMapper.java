package com.campaignorganizer.characters.application.template.service;

import com.campaignorganizer.characters.application.template.port.published.FieldTemplateView;
import com.campaignorganizer.characters.domain.template.FieldTemplate;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FieldTemplateViewMapper {

    FieldTemplateView toView(FieldTemplate template);
}
