package com.campaignorganizer.characters.application.template.port.in;

import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateView;
import java.util.UUID;

public interface GetGlobalFieldTemplateUseCase {

    GlobalFieldTemplateView get(UUID templateId);
}
