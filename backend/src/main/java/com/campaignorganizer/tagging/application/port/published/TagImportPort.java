package com.campaignorganizer.tagging.application.port.published;

/** Published import for backup/restore (FR-36): saves a tag verbatim. */
public interface TagImportPort {

    TagView importTag(TagView view);
}
