package com.campaignorganizer.characters.adapter.sheet.out.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Fills a bundled fillable PDF from a character sheet's values (ADR-0028).
 * Currently supports the D&D 5e (WotC) sheet; other systems are unsupported.
 */
@Service
public class CharacterSheetPdfService {

    private static final String DND5E_TEMPLATE = "pdf/dnd5e-character-sheet.pdf";

    public boolean supports(String system) {
        return "dnd5e".equalsIgnoreCase(system);
    }

    /** Fill the sheet into its system's PDF and return the bytes (kept fillable). */
    public byte[] fill(String system, String characterName, Map<String, Object> values) {
        if (!supports(system)) {
            throw new IllegalArgumentException("No PDF template for system: " + system);
        }
        try (PDDocument doc = Loader.loadPDF(loadTemplate())) {
            PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
            if (form == null) {
                throw new IllegalStateException("PDF has no form fields");
            }
            fillDnd5e(form, characterName, values == null ? Map.of() : values);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to fill character sheet PDF", e);
        }
    }

    private void fillDnd5e(PDAcroForm form, String characterName, Map<String, Object> v) {
        set(form, "CharacterName", characterName);
        set(form, "ClassLevel", join(str(v.get("class")), str(v.get("level"))));
        set(form, "Race ", str(v.get("race")));
        set(form, "Alignment", str(v.get("alignment")));
        set(form, "AC", str(v.get("ac")));
        set(form, "Speed", str(v.get("speed")));
        set(form, "HPMax", str(v.get("hp")));

        set(form, "STR", str(v.get("str")));
        set(form, "DEX", str(v.get("dex")));
        set(form, "CON", str(v.get("con")));
        set(form, "INT", str(v.get("int")));
        set(form, "WIS", str(v.get("wis")));
        set(form, "CHA", str(v.get("cha")));

        // Computed ability modifiers (field names carry the sheet's quirks).
        set(form, "STRmod", modifier(v.get("str")));
        set(form, "DEXmod ", modifier(v.get("dex")));
        set(form, "CONmod", modifier(v.get("con")));
        set(form, "INTmod", modifier(v.get("int")));
        set(form, "WISmod", modifier(v.get("wis")));
        set(form, "CHamod", modifier(v.get("cha")));

        set(form, "Features and Traits", str(v.get("features")));
        set(form, "Equipment", str(v.get("equipment")));
    }

    /** Set a field if it exists; ignore unknown fields and per-field failures. */
    private void set(PDAcroForm form, String fieldName, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        PDField field = form.getField(fieldName);
        if (field == null) {
            return;
        }
        try {
            field.setValue(value);
        } catch (IOException | IllegalArgumentException ignored) {
            // Best-effort: a value the field rejects should not fail the whole export.
        }
    }

    private byte[] loadTemplate() {
        try (InputStream in = new ClassPathResource(DND5E_TEMPLATE).getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("PDF template not found: " + DND5E_TEMPLATE, e);
        }
    }

    private static String str(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
            return String.valueOf(n.longValue()); // avoid "5.0"
        }
        return value.toString();
    }

    private static String join(String a, String b) {
        if (a == null && b == null) {
            return null;
        }
        return ((a == null ? "" : a) + " " + (b == null ? "" : b)).trim();
    }

    /** D&D ability modifier, signed (e.g. 16 -> "+3", 8 -> "-1"). */
    private static String modifier(Object score) {
        if (!(score instanceof Number)) {
            if (score == null) {
                return null;
            }
            try {
                score = Integer.parseInt(score.toString().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        int mod = Math.floorDiv(((Number) score).intValue() - 10, 2);
        return (mod >= 0 ? "+" : "") + mod;
    }
}
