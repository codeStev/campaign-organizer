package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * One-time consolidation (FR-55, ADR-0093): groups existing field_templates
 * rows by (kind, lower(trim(system))); for every group spanning more than
 * one world, creates one canonical global_field_templates row (seeded from
 * the most-recently-updated member), repoints every character_sheets and
 * statblocks row referencing any template in the group to the new global
 * id, then deletes the group's field_templates rows. Groups confined to a
 * single world are left untouched — that's not duplication.
 *
 * <p>A Java migration (the first in this repo, every prior one is plain
 * SQL) is used deliberately: this is a genuine one-time data
 * transformation (grouping, canonical selection, FK repointing, row
 * deletion), not schema DDL, and Flyway's own versioned run-exactly-once
 * guarantee is the right tool for that — see ADR-0093's "One-time
 * consolidation" section.
 */
public class V40__ConsolidateDuplicateFieldTemplates extends BaseJavaMigration {

    private record TemplateRow(UUID id, UUID worldId, String name, String kind, String system,
                                String sectionsJson, Timestamp updatedAt) {
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        List<TemplateRow> rows = loadTemplates(conn);

        Map<String, List<TemplateRow>> groups = new HashMap<>();
        for (TemplateRow r : rows) {
            if (r.system() == null || r.system().isBlank()) {
                continue;
            }
            String key = r.kind() + "::" + r.system().trim().toLowerCase();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        for (List<TemplateRow> group : groups.values()) {
            long distinctWorlds = group.stream().map(TemplateRow::worldId).distinct().count();
            if (distinctWorlds < 2) {
                continue;
            }

            TemplateRow seed = group.stream()
                    .max((a, b) -> a.updatedAt().compareTo(b.updatedAt()))
                    .orElseThrow();

            UUID globalId = UUID.randomUUID();
            insertGlobalTemplate(conn, globalId, seed);

            for (TemplateRow r : group) {
                repointCharacterSheets(conn, r.id(), globalId);
                repointStatblocks(conn, r.id(), globalId);
            }
            for (TemplateRow r : group) {
                deleteTemplate(conn, r.id());
            }
        }
    }

    private List<TemplateRow> loadTemplates(Connection conn) throws Exception {
        List<TemplateRow> out = new ArrayList<>();
        String sql = "SELECT id, world_id, name, kind, system, sections::text, updated_at "
                + "FROM field_templates WHERE kind IN ('CHARACTER','STATBLOCK')";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new TemplateRow(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("world_id")),
                        rs.getString("name"),
                        rs.getString("kind"),
                        rs.getString("system"),
                        rs.getString("sections"),
                        rs.getTimestamp("updated_at")));
            }
        }
        return out;
    }

    private void insertGlobalTemplate(Connection conn, UUID id, TemplateRow seed) throws Exception {
        String sql = "INSERT INTO global_field_templates (id, name, kind, system, sections, created_at, "
                + "updated_at) VALUES (?, ?, ?, ?, ?::jsonb, now(), now())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setString(2, seed.name());
            ps.setString(3, seed.kind());
            ps.setString(4, seed.system().trim());
            ps.setString(5, seed.sectionsJson());
            ps.executeUpdate();
        }
    }

    private void repointCharacterSheets(Connection conn, UUID oldTemplateId, UUID globalId) throws Exception {
        String sql = "UPDATE character_sheets SET world_template_id = NULL, global_template_id = ? "
                + "WHERE world_template_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, globalId);
            ps.setObject(2, oldTemplateId);
            ps.executeUpdate();
        }
    }

    private void repointStatblocks(Connection conn, UUID oldTemplateId, UUID globalId) throws Exception {
        String sql = "UPDATE statblocks SET world_template_id = NULL, global_template_id = ? "
                + "WHERE world_template_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, globalId);
            ps.setObject(2, oldTemplateId);
            ps.executeUpdate();
        }
    }

    private void deleteTemplate(Connection conn, UUID id) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM field_templates WHERE id = ?")) {
            ps.setObject(1, id);
            ps.executeUpdate();
        }
    }
}
