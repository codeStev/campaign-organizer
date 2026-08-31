package com.campaignorganizer.tagging.application.port.in;

import com.campaignorganizer.tagging.application.port.in.TagCommands.SetEntityTagsCommand;
import java.util.List;

public interface SetEntityTagsUseCase {

    /** Replaces the entity's whole tag set; returns the resulting tags, alphabetical. */
    List<String> set(SetEntityTagsCommand command);
}
