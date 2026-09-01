package com.campaignorganizer.characters.application.template.port.in;

import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateView;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import java.util.List;

public interface ListGlobalFieldTemplatesUseCase {

    /** Lists the global catalog; when {@code kind} is set, scopes to that kind. */
    List<GlobalFieldTemplateView> list(TemplateKind kind);
}
