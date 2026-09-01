package com.campaignorganizer.characters.application.document.port.in;

import com.campaignorganizer.characters.application.document.port.in.DocumentCommands.CreateDocumentCommand;
import com.campaignorganizer.characters.application.document.port.published.DocumentView;

public interface CreateDocumentUseCase {

    DocumentView create(CreateDocumentCommand command);
}
