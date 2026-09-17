package com.campaignorganizer.interchange.foundry.domain;

/** How a wiki category's articles are laid out in Foundry when the whole category is pushed
 * at once (ADR-0115 addendum) — the user picks one at push time; neither is a default the
 * other falls back to. */
public enum FoundryCategoryPushMode {
    /** One Foundry subfolder per category (nested to match subcategory structure), each
     * article pushed as its own separate JournalEntry document inside it — identical to
     * pushing every article in the category individually, just done in one action. */
    FOLDER,
    /** One JournalEntry document named after the category, with one page per article
     * (subcategory articles included, page-named "Subcategory / Article" to keep the
     * grouping visible in Foundry's flat page list). */
    SINGLE_DOCUMENT
}
