package com.campaignorganizer.ai.adapter.in.web;

import com.campaignorganizer.ai.domain.DraftResult;
import org.mapstruct.Mapper;

/** Maps the domain result to the web response (MapStruct). */
@Mapper(componentModel = "spring")
public interface AiWebMapper {

    DraftArticleTextResponse toResponse(DraftResult result);
}
