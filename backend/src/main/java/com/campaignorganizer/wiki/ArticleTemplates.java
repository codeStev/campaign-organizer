package com.campaignorganizer.wiki;

import java.util.List;

/**
 * Static definitions of the structured prompts for each {@link ArticleTemplate}
 * (see ADR-0015). Application configuration, not user data.
 */
public final class ArticleTemplates {

    public record TemplateSection(String heading, String hint) {
    }

    public record ArticleTemplateInfo(ArticleTemplate template, String label, List<TemplateSection> sections) {
    }

    private ArticleTemplates() {
    }

    public static List<ArticleTemplateInfo> all() {
        return List.of(
                info(ArticleTemplate.GENERIC,
                        section("Overview", "A short summary of the subject."),
                        section("Details", "The important specifics."),
                        section("History", "How it came to be.")),
                info(ArticleTemplate.CHARACTER,
                        section("Appearance", "Physical description and distinguishing features."),
                        section("Personality", "Temperament, values, and quirks."),
                        section("History", "Background and key life events."),
                        section("Goals & Motivations", "What drives them."),
                        section("Relationships", "Allies, rivals, and connections.")),
                info(ArticleTemplate.LOCATION,
                        section("Geography", "Terrain, climate, and layout."),
                        section("Demographics", "Who lives here and how many."),
                        section("History", "Founding and notable events."),
                        section("Points of Interest", "Landmarks and places to visit.")),
                info(ArticleTemplate.ORGANIZATION,
                        section("Purpose", "What the organization exists to do."),
                        section("Structure", "Ranks, roles, and hierarchy."),
                        section("History", "Origins and turning points."),
                        section("Notable Members", "Key figures.")),
                info(ArticleTemplate.SPECIES,
                        section("Biology", "Physiology and lifecycle."),
                        section("Behavior", "Habits and temperament."),
                        section("Habitat", "Where they are found."),
                        section("Culture", "Society and beliefs, if any.")),
                info(ArticleTemplate.ITEM,
                        section("Description", "What it looks like."),
                        section("Properties", "Abilities, materials, and mechanics."),
                        section("History", "Origin and past owners."),
                        section("Current Owner", "Who holds it now.")),
                info(ArticleTemplate.EVENT,
                        section("Summary", "What happened."),
                        section("Causes", "What led to it."),
                        section("Consequences", "What resulted."),
                        section("Participants", "Who was involved.")));
    }

    private static ArticleTemplateInfo info(ArticleTemplate template, TemplateSection... sections) {
        return new ArticleTemplateInfo(template, label(template), List.of(sections));
    }

    private static TemplateSection section(String heading, String hint) {
        return new TemplateSection(heading, hint);
    }

    private static String label(ArticleTemplate template) {
        String name = template.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
