package com.campaignorganizer.interchange.foundry.adapter.in.web;

import com.campaignorganizer.interchange.foundry.adapter.in.web.FoundryPushWebDtos.FoundryCampaignPushResponse;
import com.campaignorganizer.interchange.foundry.adapter.in.web.FoundryPushWebDtos.FoundryCategoryPushResponse;
import com.campaignorganizer.interchange.foundry.adapter.in.web.FoundryPushWebDtos.FoundryPushResponse;
import com.campaignorganizer.interchange.foundry.adapter.in.web.FoundryPushWebDtos.FoundryPushStatusResponse;
import com.campaignorganizer.interchange.foundry.adapter.in.web.FoundryPushWebDtos.FoundrySessionPushResponse;
import com.campaignorganizer.interchange.foundry.application.port.in.GetFoundryPushStatusUseCase.FoundryPushStatusView;
import com.campaignorganizer.interchange.foundry.application.port.in.PushArticleToFoundryUseCase.FoundryPushResult;
import com.campaignorganizer.interchange.foundry.application.port.in.PushCampaignToFoundryUseCase.FoundryCampaignPushResult;
import com.campaignorganizer.interchange.foundry.application.port.in.PushCategoryToFoundryUseCase.FoundryCategoryPushResult;
import com.campaignorganizer.interchange.foundry.application.port.in.PushSessionToFoundryUseCase.FoundrySessionPushResult;
import java.util.Optional;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FoundryPushWebMapper {

    FoundryPushResponse toResponse(FoundryPushResult result);

    FoundrySessionPushResponse toSessionResponse(FoundrySessionPushResult result);

    FoundryCategoryPushResponse toCategoryResponse(FoundryCategoryPushResult result);

    FoundryCampaignPushResponse toCampaignResponse(FoundryCampaignPushResult result);

    default FoundryPushStatusResponse toStatusResponse(Optional<FoundryPushStatusView> view) {
        return view
                .map(v -> new FoundryPushStatusResponse(true, v.foundryDocumentId(), v.pushedAt()))
                .orElseGet(() -> new FoundryPushStatusResponse(false, null, null));
    }
}
