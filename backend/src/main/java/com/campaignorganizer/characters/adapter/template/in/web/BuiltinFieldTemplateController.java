package com.campaignorganizer.characters.adapter.template.in.web;

import com.campaignorganizer.characters.domain.template.BuiltinFieldTemplates;
import com.campaignorganizer.characters.domain.template.BuiltinFieldTemplates.BuiltinTemplate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Serves the built-in starter field templates (world-independent reference data). */
@RestController
@RequestMapping("/api/field-templates/builtin")
public class BuiltinFieldTemplateController {

    @GetMapping
    public List<BuiltinTemplate> list() {
        return BuiltinFieldTemplates.all();
    }
}
