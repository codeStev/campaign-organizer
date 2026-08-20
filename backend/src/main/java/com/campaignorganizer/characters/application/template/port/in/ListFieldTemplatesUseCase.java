package com.campaignorganizer.characters.application.template.port.in;

import com.campaignorganizer.characters.application.template.port.published.FieldTemplateView;
import java.util.List;
import java.util.UUID;

public interface ListFieldTemplatesUseCase {

    List<FieldTemplateView> list(UUID worldId);
}
