package com.campaignorganizer.characters.application.template.port.in;

import com.campaignorganizer.characters.application.template.port.published.FieldTemplateView;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import java.util.List;
import java.util.UUID;

public interface ListFieldTemplatesUseCase {

    /** Lists a world's field templates; when {@code kind} is set, scopes to that kind. */
    List<FieldTemplateView> list(UUID worldId, TemplateKind kind);
}
