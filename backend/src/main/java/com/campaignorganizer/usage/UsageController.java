package com.campaignorganizer.usage;

import com.campaignorganizer.usage.UsageDtos.UsageResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/worlds/{worldId}/articles/{articleId}/usages")
public class UsageController {

    private final UsageService usage;

    public UsageController(UsageService usage) {
        this.usage = usage;
    }

    @GetMapping
    public UsageResponse list(@PathVariable UUID worldId, @PathVariable UUID articleId) {
        return usage.articleUsages(worldId, articleId);
    }
}
