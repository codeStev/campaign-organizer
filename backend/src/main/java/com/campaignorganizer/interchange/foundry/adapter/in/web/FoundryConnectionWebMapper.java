package com.campaignorganizer.interchange.foundry.adapter.in.web;

import com.campaignorganizer.interchange.foundry.adapter.in.web.FoundryConnectionWebDtos.FoundryConnectionRequest;
import com.campaignorganizer.interchange.foundry.adapter.in.web.FoundryConnectionWebDtos.FoundryConnectionResponse;
import com.campaignorganizer.interchange.foundry.adapter.in.web.FoundryConnectionWebDtos.FoundryConnectionTestResponse;
import com.campaignorganizer.interchange.foundry.application.port.in.FoundryConnectionView;
import com.campaignorganizer.interchange.foundry.application.port.in.SaveFoundryConnectionUseCase.SaveFoundryConnectionCommand;
import com.campaignorganizer.interchange.foundry.application.port.in.TestFoundryConnectionUseCase.FoundryConnectionTestView;
import java.util.Optional;
import org.mapstruct.Mapper;

/** Maps Foundry connection web DTOs ↔ commands/views (MapStruct). Never maps
 * an API key into any response — {@link FoundryConnectionView} carries none. */
@Mapper(componentModel = "spring")
public interface FoundryConnectionWebMapper {

    FoundryConnectionResponse toResponse(FoundryConnectionView view);

    FoundryConnectionTestResponse toTestResponse(FoundryConnectionTestView view);

    default SaveFoundryConnectionCommand toCommand(FoundryConnectionRequest request) {
        Optional<String> apiKey = request.apiKey() == null || request.apiKey().isBlank()
                ? Optional.empty()
                : Optional.of(request.apiKey());
        return new SaveFoundryConnectionCommand(request.relayBaseUrl(), request.clientId(), apiKey);
    }
}
