package com.campaignorganizer.characters.application.template.port.in;

import com.campaignorganizer.characters.application.template.port.in.GlobalFieldTemplateCommands.CreateGlobalFieldTemplateCommand;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateView;

public interface CreateGlobalFieldTemplateUseCase {

    GlobalFieldTemplateView create(CreateGlobalFieldTemplateCommand command);
}
