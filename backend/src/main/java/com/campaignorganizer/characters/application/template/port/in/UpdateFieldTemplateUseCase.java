package com.campaignorganizer.characters.application.template.port.in;

import com.campaignorganizer.characters.application.template.port.in.FieldTemplateCommands.UpdateFieldTemplateCommand;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateView;

public interface UpdateFieldTemplateUseCase {

    FieldTemplateView update(UpdateFieldTemplateCommand command);
}
