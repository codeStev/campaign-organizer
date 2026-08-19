package com.campaignorganizer.characters.domain.sheet;

import java.util.List;

/** JSON-serializable definition of a sheet's fields (see ADR-0024). */
public final class SheetSchema {

    private SheetSchema() {
    }

    public enum FieldType {
        TEXT,
        TEXTAREA,
        NUMBER,
        BOOLEAN,
        SELECT,
        CIRCLES
    }

    /**
     * A single field within a section.
     * {@code options} is used by SELECT; {@code count} by CIRCLES;
     * {@code width} (FULL/HALF/THIRD/QUARTER, default FULL) controls side-by-side
     * layout. See ADR-0030.
     */
    public record SheetField(String key, String label, FieldType type, List<String> options,
                             String width, Integer count) {
    }

    /** A titled group of fields. */
    public record SheetSection(String title, List<SheetField> fields) {
    }
}
