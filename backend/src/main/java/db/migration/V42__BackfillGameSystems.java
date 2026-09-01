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
 * One-time backfill (FR-56, ADR-0094): groups every existing {@code system}
 * string across both {@code field_templates} and {@code global_field_templates}
 * by {@code trim(lower(system))}, creates one {@code game_systems} row per
 * distinct value (display name from the most-recently-updated occurrence's
 * original casing), sets {@code system_id} on every matching row in both
 * tables, then tightens {@code global_field_templates.system_id} to
 * {@code NOT NULL} and drops both tables' now-redundant {@code system}
 * columns. One backfill covering both tables so a "homebrew" field template
 * and a "homebrew" global template resolve to the same game system.
 *
 * <p>The second Java migration in this repo after {@code V40} — same
 * reasoning: a genuine one-time data transformation, not schema DDL.
 */
public class V42__BackfillGameSystems extends BaseJavaMigration {

    private enum Table { FIELD_TEMPLATES, GLOBAL_FIELD_TEMPLATES }

    private record SystemRow(Table table, UUID id, String system, Timestamp updatedAt) {
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        List<SystemRow> rows = new ArrayList<>();
        rows.addAll(loadFieldTemplateSystems(conn));
        rows.addAll(loadGlobalFieldTemplateSystems(conn));

        Map<String, List<SystemRow>> groups = new HashMap<>();
        for (SystemRow r : rows) {
            if (r.system() == null || r.system().isBlank()) {
                continue;
            }
            groups.computeIfAbsent(r.system().trim().toLowerCase(), k -> new ArrayList<>()).add(r);
        }

        for (List<SystemRow> group : groups.values()) {
            SystemRow seed = group.stream()
                    .max((a, b) -> a.updatedAt().compareTo(b.updatedAt()))
                    .orElseThrow();
            UUID systemId = UUID.randomUUID();
            insertGameSystem(conn, systemId, seed.system().trim());
            for (SystemRow r : group) {
                if (r.table() == Table.FIELD_TEMPLATES) {
                    setFieldTemplateSystemId(conn, r.id(), systemId);
                } else {
                    setGlobalFieldTemplateSystemId(conn, r.id(), systemId);
                }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "ALTER TABLE global_field_templates ALTER COLUMN system_id SET NOT NULL")) {
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "ALTER TABLE field_templates DROP COLUMN system")) {
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "ALTER TABLE global_field_templates DROP COLUMN system")) {
            ps.executeUpdate();
        }
    }

    private List<SystemRow> loadFieldTemplateSystems(Connection conn) throws Exception {
        List<SystemRow> out = new ArrayList<>();
        String sql = "SELECT id, system, updated_at FROM field_templates";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new SystemRow(Table.FIELD_TEMPLATES, UUID.fromString(rs.getString("id")),
                        rs.getString("system"), rs.getTimestamp("updated_at")));
            }
        }
        return out;
    }

    private List<SystemRow> loadGlobalFieldTemplateSystems(Connection conn) throws Exception {
        List<SystemRow> out = new ArrayList<>();
        String sql = "SELECT id, system, updated_at FROM global_field_templates";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new SystemRow(Table.GLOBAL_FIELD_TEMPLATES, UUID.fromString(rs.getString("id")),
                        rs.getString("system"), rs.getTimestamp("updated_at")));
            }
        }
        return out;
    }

    private void insertGameSystem(Connection conn, UUID id, String name) throws Exception {
        String sql = "INSERT INTO game_systems (id, name, created_at, updated_at) VALUES (?, ?, now(), now())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setString(2, name);
            ps.executeUpdate();
        }
    }

    private void setFieldTemplateSystemId(Connection conn, UUID rowId, UUID systemId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE field_templates SET system_id = ? WHERE id = ?")) {
            ps.setObject(1, systemId);
            ps.setObject(2, rowId);
            ps.executeUpdate();
        }
    }

    private void setGlobalFieldTemplateSystemId(Connection conn, UUID rowId, UUID systemId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE global_field_templates SET system_id = ? WHERE id = ?")) {
            ps.setObject(1, systemId);
            ps.setObject(2, rowId);
            ps.executeUpdate();
        }
    }
}
