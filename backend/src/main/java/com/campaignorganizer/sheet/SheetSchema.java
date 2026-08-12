package com.campaignorganizer.sheet;

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
        SELECT
    }

    /** A single field within a section. {@code options} is used by SELECT. */
    public record SheetField(String key, String label, FieldType type, List<String> options) {
    }

    /** A titled group of fields. */
    public record SheetSection(String title, List<SheetField> fields) {
    }
}
