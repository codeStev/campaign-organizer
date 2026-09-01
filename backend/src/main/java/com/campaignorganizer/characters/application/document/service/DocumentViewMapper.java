package com.campaignorganizer.characters.application.document.service;

import com.campaignorganizer.characters.application.document.port.published.DocumentView;
import com.campaignorganizer.characters.domain.document.Document;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentViewMapper {

    DocumentView toView(Document document);
}
