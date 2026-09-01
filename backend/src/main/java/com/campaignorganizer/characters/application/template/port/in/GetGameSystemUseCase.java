package com.campaignorganizer.characters.application.template.port.in;

import com.campaignorganizer.characters.application.template.port.published.GameSystemView;
import java.util.UUID;

public interface GetGameSystemUseCase {

    GameSystemView get(UUID systemId);
}
