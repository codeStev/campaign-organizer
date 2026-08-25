package com.campaignorganizer.ai.adapter.in.web;

import com.campaignorganizer.ai.application.port.in.ProviderSettingView;
import org.mapstruct.Mapper;

/** Maps the application view to the web response (MapStruct). */
@Mapper(componentModel = "spring")
public interface AiSettingsWebMapper {

    AiProviderSettingResponse toResponse(ProviderSettingView view);
}
