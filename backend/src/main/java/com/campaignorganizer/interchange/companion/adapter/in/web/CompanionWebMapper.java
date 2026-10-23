package com.campaignorganizer.interchange.companion.adapter.in.web;

import com.campaignorganizer.interchange.companion.adapter.in.web.CompanionWebDtos.CampaignGlossaryResponse;
import com.campaignorganizer.interchange.companion.adapter.in.web.CompanionWebDtos.CompanionCampaignResponse;
import com.campaignorganizer.interchange.companion.adapter.in.web.CompanionWebDtos.GlossaryTermResponse;
import com.campaignorganizer.interchange.companion.application.port.in.GetCampaignGlossaryUseCase.CampaignGlossary;
import com.campaignorganizer.interchange.companion.application.port.in.ListCompanionCampaignsUseCase.CompanionCampaign;
import com.campaignorganizer.interchange.companion.domain.glossary.GlossaryTerm;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompanionWebMapper {

    CompanionCampaignResponse toResponse(CompanionCampaign campaign);

    CampaignGlossaryResponse toResponse(CampaignGlossary glossary);

    @Mapping(target = "kind", expression = "java(term.kind().wireValue())")
    GlossaryTermResponse toResponse(GlossaryTerm term);
}
