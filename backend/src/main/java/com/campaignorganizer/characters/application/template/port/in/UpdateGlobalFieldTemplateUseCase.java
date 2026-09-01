package com.campaignorganizer.characters.application.template.port.in;

import com.campaignorganizer.characters.application.template.port.in.GlobalFieldTemplateCommands.UpdateGlobalFieldTemplateCommand;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateView;

public interface UpdateGlobalFieldTemplateUseCase {

    GlobalFieldTemplateView update(UpdateGlobalFieldTemplateCommand command);
}
