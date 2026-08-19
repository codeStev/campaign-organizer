package com.campaignorganizer.characters.application.sheet.service;

import com.campaignorganizer.characters.application.sheet.port.published.SheetTemplateView;
import com.campaignorganizer.characters.domain.sheet.SheetTemplate;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SheetTemplateViewMapper {

    SheetTemplateView toView(SheetTemplate template);
}
