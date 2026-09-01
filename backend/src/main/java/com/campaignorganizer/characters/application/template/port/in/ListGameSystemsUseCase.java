package com.campaignorganizer.characters.application.template.port.in;

import com.campaignorganizer.characters.application.template.port.published.GameSystemView;
import java.util.List;

public interface ListGameSystemsUseCase {

    List<GameSystemView> list();
}
