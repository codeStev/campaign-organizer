package com.campaignorganizer.characters.application.template.port.in;

import java.util.UUID;

public interface DeleteGameSystemUseCase {

    void delete(UUID systemId);
}
