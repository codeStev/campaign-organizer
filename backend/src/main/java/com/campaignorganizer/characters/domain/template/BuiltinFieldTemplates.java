package com.campaignorganizer.characters.domain.template;

import com.campaignorganizer.characters.domain.template.FieldSchema.FieldType;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateField;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateSection;
import java.util.List;

/**
 * Code-defined starter field templates (FR-17, FR-34, ADR-0024, ADR-0052). The
 * user copies one into a world to get going; they are reference data, not
 * stored rows.
 */
public final class BuiltinFieldTemplates {

    public record BuiltinTemplate(String name, TemplateKind kind, String system, List<TemplateSection> sections) {
    }

    private BuiltinFieldTemplates() {
    }

    public static List<BuiltinTemplate> all() {
        return List.of(generic(), dnd5e(), dnd5eMonster());
    }

    private static BuiltinTemplate generic() {
        return new BuiltinTemplate("Generic Character", TemplateKind.CHARACTER, "generic", List.of(
                new TemplateSection("Overview", List.of(
                        field("concept", "Concept", FieldType.TEXT),
                        field("description", "Description", FieldType.TEXTAREA))),
                new TemplateSection("Traits", List.of(
                        field("strengths", "Strengths", FieldType.TEXTAREA),
                        field("flaws", "Flaws", FieldType.TEXTAREA)))));
    }

    private static BuiltinTemplate dnd5e() {
        return new BuiltinTemplate("D&D 5e", TemplateKind.CHARACTER, "dnd5e", List.of(
                new TemplateSection("Core", List.of(
                        field("class", "Class", FieldType.TEXT),
                        field("race", "Race", FieldType.TEXT),
                        field("level", "Level", FieldType.NUMBER),
                        select("alignment", "Alignment",
                                List.of("LG", "NG", "CG", "LN", "TN", "CN", "LE", "NE", "CE")))),
                new TemplateSection("Ability Scores", List.of(
                        field("str", "Strength", FieldType.NUMBER),
                        field("dex", "Dexterity", FieldType.NUMBER),
                        field("con", "Constitution", FieldType.NUMBER),
                        field("int", "Intelligence", FieldType.NUMBER),
                        field("wis", "Wisdom", FieldType.NUMBER),
                        field("cha", "Charisma", FieldType.NUMBER))),
                new TemplateSection("Combat", List.of(
                        field("ac", "Armor Class", FieldType.NUMBER),
                        field("hp", "Hit Points", FieldType.NUMBER),
                        field("speed", "Speed", FieldType.NUMBER))),
                new TemplateSection("Notes", List.of(
                        field("features", "Features & Traits", FieldType.TEXTAREA),
                        field("equipment", "Equipment", FieldType.TEXTAREA)))));
    }

    private static BuiltinTemplate dnd5eMonster() {
        return new BuiltinTemplate("D&D 5e Monster", TemplateKind.STATBLOCK, "dnd5e", List.of(
                new TemplateSection("Overview", List.of(
                        field("type", "Type & Alignment", FieldType.TEXT),
                        fieldW("cr", "Challenge Rating", FieldType.TEXT, "QUARTER"),
                        fieldW("pb", "Proficiency Bonus", FieldType.NUMBER, "QUARTER"))),
                new TemplateSection("Defenses", List.of(
                        fieldW("ac", "Armor Class", FieldType.NUMBER, "THIRD"),
                        fieldW("hp", "Hit Points", FieldType.NUMBER, "THIRD"),
                        fieldW("speed", "Speed", FieldType.TEXT, "THIRD"))),
                new TemplateSection("Ability Scores", List.of(
                        fieldW("str", "STR", FieldType.NUMBER, "QUARTER"),
                        fieldW("dex", "DEX", FieldType.NUMBER, "QUARTER"),
                        fieldW("con", "CON", FieldType.NUMBER, "QUARTER"),
                        fieldW("int", "INT", FieldType.NUMBER, "QUARTER"),
                        fieldW("wis", "WIS", FieldType.NUMBER, "QUARTER"),
                        fieldW("cha", "CHA", FieldType.NUMBER, "QUARTER"))),
                new TemplateSection("Senses & Languages", List.of(
                        fieldW("senses", "Senses", FieldType.TEXT, "HALF"),
                        fieldW("languages", "Languages", FieldType.TEXT, "HALF"),
                        circles("legendary_resistances", "Legendary Resistances", 3))),
                new TemplateSection("Traits & Actions", List.of(
                        field("traits", "Traits", FieldType.TEXTAREA),
                        field("actions", "Actions", FieldType.TEXTAREA),
                        field("reactions", "Reactions", FieldType.TEXTAREA),
                        field("legendary_actions", "Legendary Actions", FieldType.TEXTAREA)))));
    }

    private static TemplateField field(String key, String label, FieldType type) {
        return new TemplateField(key, label, type, null, null, null);
    }

    private static TemplateField fieldW(String key, String label, FieldType type, String width) {
        return new TemplateField(key, label, type, null, width, null);
    }

    private static TemplateField select(String key, String label, List<String> options) {
        return new TemplateField(key, label, FieldType.SELECT, options, null, null);
    }

    private static TemplateField circles(String key, String label, int count) {
        return new TemplateField(key, label, FieldType.CIRCLES, null, null, count);
    }
}
