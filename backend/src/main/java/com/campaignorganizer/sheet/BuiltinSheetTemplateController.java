package com.campaignorganizer.sheet;

import com.campaignorganizer.sheet.BuiltinSheetTemplates.BuiltinTemplate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Serves the built-in starter sheet templates (world-independent reference data). */
@RestController
@RequestMapping("/api/sheet-templates/builtin")
public class BuiltinSheetTemplateController {

    @GetMapping
    public List<BuiltinTemplate> list() {
        return BuiltinSheetTemplates.all();
    }
}
