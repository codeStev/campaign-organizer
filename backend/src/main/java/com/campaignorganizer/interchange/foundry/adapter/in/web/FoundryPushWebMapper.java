package com.campaignorganizer.interchange.foundry.adapter.in.web;

import com.campaignorganizer.interchange.foundry.adapter.in.web.FoundryPushWebDtos.FoundryPushResponse;
import com.campaignorganizer.interchange.foundry.adapter.in.web.FoundryPushWebDtos.FoundryPushStatusResponse;
import com.campaignorganizer.interchange.foundry.application.port.in.GetFoundryPushStatusUseCase.FoundryPushStatusView;
import com.campaignorganizer.interchange.foundry.application.port.in.PushArticleToFoundryUseCase.FoundryPushResult;
import java.util.Optional;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FoundryPushWebMapper {

    FoundryPushResponse toResponse(FoundryPushResult result);

    default FoundryPushStatusResponse toStatusResponse(Optional<FoundryPushStatusView> view) {
        return view
                .map(v -> new FoundryPushStatusResponse(true, v.foundryDocumentId(), v.pushedAt()))
                .orElseGet(() -> new FoundryPushStatusResponse(false, null, null));
    }
}
