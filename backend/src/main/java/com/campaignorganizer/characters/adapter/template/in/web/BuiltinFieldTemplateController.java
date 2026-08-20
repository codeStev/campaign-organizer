package com.campaignorganizer.characters.adapter.template.in.web;

import com.campaignorganizer.characters.domain.template.BuiltinFieldTemplates;
import com.campaignorganizer.characters.domain.template.BuiltinFieldTemplates.BuiltinTemplate;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Serves the built-in starter field templates (world-independent reference data). */
@RestController
@RequestMapping("/api/field-templates/builtin")
public class BuiltinFieldTemplateController {

    @GetMapping
    public List<BuiltinTemplate> list(@RequestParam(required = false) TemplateKind kind) {
        List<BuiltinTemplate> all = BuiltinFieldTemplates.all();
        return kind == null ? all : all.stream().filter(t -> t.kind() == kind).toList();
    }
}
