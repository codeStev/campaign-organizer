package com.campaignorganizer.characters.application.template.port.in;

import com.campaignorganizer.characters.application.template.port.in.FieldTemplateCommands.CreateFieldTemplateCommand;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateView;

public interface CreateFieldTemplateUseCase {

    FieldTemplateView create(CreateFieldTemplateCommand command);
}
