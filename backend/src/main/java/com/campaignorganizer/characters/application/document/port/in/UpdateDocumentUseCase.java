package com.campaignorganizer.characters.application.document.port.in;

import com.campaignorganizer.characters.application.document.port.in.DocumentCommands.UpdateDocumentCommand;
import com.campaignorganizer.characters.application.document.port.published.DocumentView;

public interface UpdateDocumentUseCase {

    DocumentView update(UpdateDocumentCommand command);
}
