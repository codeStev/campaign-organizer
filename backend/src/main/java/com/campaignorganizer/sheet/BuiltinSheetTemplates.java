package com.campaignorganizer.sheet;

import com.campaignorganizer.sheet.SheetSchema.FieldType;
import com.campaignorganizer.sheet.SheetSchema.SheetField;
import com.campaignorganizer.sheet.SheetSchema.SheetSection;
import java.util.List;

/**
 * Code-defined starter sheet templates (FR-17, ADR-0024). The user copies one
 * into a world to get going; they are reference data, not stored rows.
 */
public final class BuiltinSheetTemplates {

    public record BuiltinTemplate(String name, String system, List<SheetSection> sections) {
    }

    private BuiltinSheetTemplates() {
    }

    public static List<BuiltinTemplate> all() {
        return List.of(generic(), dnd5e());
    }

    private static BuiltinTemplate generic() {
        return new BuiltinTemplate("Generic Character", "generic", List.of(
                new SheetSection("Overview", List.of(
                        field("concept", "Concept", FieldType.TEXT),
                        field("description", "Description", FieldType.TEXTAREA))),
                new SheetSection("Traits", List.of(
                        field("strengths", "Strengths", FieldType.TEXTAREA),
                        field("flaws", "Flaws", FieldType.TEXTAREA)))));
    }

    private static BuiltinTemplate dnd5e() {
        return new BuiltinTemplate("D&D 5e", "dnd5e", List.of(
                new SheetSection("Core", List.of(
                        field("class", "Class", FieldType.TEXT),
                        field("race", "Race", FieldType.TEXT),
                        field("level", "Level", FieldType.NUMBER),
                        select("alignment", "Alignment",
                                List.of("LG", "NG", "CG", "LN", "TN", "CN", "LE", "NE", "CE")))),
                new SheetSection("Ability Scores", List.of(
                        field("str", "Strength", FieldType.NUMBER),
                        field("dex", "Dexterity", FieldType.NUMBER),
                        field("con", "Constitution", FieldType.NUMBER),
                        field("int", "Intelligence", FieldType.NUMBER),
                        field("wis", "Wisdom", FieldType.NUMBER),
                        field("cha", "Charisma", FieldType.NUMBER))),
                new SheetSection("Combat", List.of(
                        field("ac", "Armor Class", FieldType.NUMBER),
                        field("hp", "Hit Points", FieldType.NUMBER),
                        field("speed", "Speed", FieldType.NUMBER))),
                new SheetSection("Notes", List.of(
                        field("features", "Features & Traits", FieldType.TEXTAREA),
                        field("equipment", "Equipment", FieldType.TEXTAREA)))));
    }

    private static SheetField field(String key, String label, FieldType type) {
        return new SheetField(key, label, type, null);
    }

    private static SheetField select(String key, String label, List<String> options) {
        return new SheetField(key, label, FieldType.SELECT, options);
    }
}
