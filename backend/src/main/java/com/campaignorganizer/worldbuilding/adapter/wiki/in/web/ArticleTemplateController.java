package com.campaignorganizer.worldbuilding.adapter.wiki.in.web;

import com.campaignorganizer.worldbuilding.domain.wiki.ArticleTemplates;
import com.campaignorganizer.worldbuilding.domain.wiki.ArticleTemplates.ArticleTemplateInfo;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/article-templates")
public class ArticleTemplateController {

    @GetMapping
    public List<ArticleTemplateInfo> list() {
        return ArticleTemplates.all();
    }
}
