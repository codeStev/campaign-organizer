package com.campaignorganizer.characters.application.template.port.in;

import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateView;
import java.util.UUID;

/** Promotes a world-scoped template to the global catalog (ADR-0093). */
public interface PromoteFieldTemplateUseCase {

    GlobalFieldTemplateView promote(UUID worldId, UUID templateId);
}
