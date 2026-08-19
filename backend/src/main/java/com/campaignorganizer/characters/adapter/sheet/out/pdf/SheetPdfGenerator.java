package com.campaignorganizer.characters.adapter.sheet.out.pdf;

import com.campaignorganizer.characters.domain.sheet.SheetSchema.FieldType;
import com.campaignorganizer.characters.domain.sheet.SheetSchema.SheetField;
import com.campaignorganizer.characters.domain.sheet.SheetSchema.SheetSection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceCharacteristicsDictionary;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.springframework.stereotype.Service;

/**
 * Builds a fillable AcroForm PDF from an arbitrary sheet template (ADR-0029,
 * ADR-0030). Fields pack into rows by their width (side by side); CIRCLES render
 * as a row of checkboxes. The output stays fillable.
 */
@Service
public class SheetPdfGenerator {

    private static final float MARGIN = 50;
    private static final float LABEL_SIZE = 9;
    private static final float FIELD_H = 16;
    private static final float TEXTAREA_H = 54;
    private static final float CIRCLE = 12;
    private static final float CIRCLE_GAP = 4;
    private static final float COL_GAP = 10;
    private static final float ROW_GAP = 8;

    private final PDFont font = new PDType1Font(FontName.HELVETICA);
    private final PDFont bold = new PDType1Font(FontName.HELVETICA_BOLD);

    public byte[] generate(String title, List<SheetSection> sections, Map<String, Object> values) {
        Map<String, Object> v = values == null ? Map.of() : values;
        try (PDDocument doc = new PDDocument()) {
            PDAcroForm form = new PDAcroForm(doc);
            doc.getDocumentCatalog().setAcroForm(form);
            PDResources dr = new PDResources();
            dr.put(COSName.getPDFName("Helv"), font);
            form.setDefaultResources(dr);
            form.setDefaultAppearance("/Helv 0 Tf 0 g");

            Layout layout = new Layout(doc, form);
            layout.title(title == null ? "Character Sheet" : title);
            Set<String> usedKeys = new HashSet<>();

            for (SheetSection section : sections == null ? List.<SheetSection>of() : sections) {
                layout.heading(section.title());
                List<SheetField> row = new ArrayList<>();
                int usedCols = 0;
                for (SheetField f : section.fields() == null ? List.<SheetField>of() : section.fields()) {
                    int span = spanOf(f.width());
                    if (usedCols + span > 12 && !row.isEmpty()) {
                        layout.row(row, v, usedKeys);
                        row.clear();
                        usedCols = 0;
                    }
                    row.add(f);
                    usedCols += span;
                }
                if (!row.isEmpty()) {
                    layout.row(row, v, usedKeys);
                }
            }
            layout.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate sheet PDF", e);
        }
    }

    private static int spanOf(String width) {
        if (width == null) {
            return 12;
        }
        return switch (width.toUpperCase()) {
            case "QUARTER" -> 3;
            case "THIRD" -> 4;
            case "HALF" -> 6;
            default -> 12;
        };
    }

    private static float widgetHeight(SheetField f) {
        return f.type() == FieldType.TEXTAREA ? TEXTAREA_H : FIELD_H;
    }

    private static String uniqueKey(String key, Set<String> used) {
        String base = (key == null || key.isBlank()) ? "field" : key;
        String candidate = base;
        int i = 2;
        while (!used.add(candidate)) {
            candidate = base + "_" + i++;
        }
        return candidate;
    }

    /** Cursor-based layout that renders one row of side-by-side cells at a time. */
    private final class Layout {
        private final PDDocument doc;
        private final PDAcroForm form;
        private PDPage page;
        private PDPageContentStream cs;
        private float y;

        Layout(PDDocument doc, PDAcroForm form) throws IOException {
            this.doc = doc;
            this.form = form;
            newPage();
        }

        private void newPage() throws IOException {
            if (cs != null) {
                cs.close();
            }
            page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        void title(String text) throws IOException {
            drawText(text, bold, 16, MARGIN, y);
            y -= 26;
        }

        void heading(String text) throws IOException {
            if (y - 24 < MARGIN) {
                newPage();
            }
            y -= 6;
            drawText(text == null ? "" : text, bold, 12, MARGIN, y);
            cs.moveTo(MARGIN, y - 4);
            cs.lineTo(MARGIN + contentWidth(), y - 4);
            cs.stroke();
            y -= 18;
        }

        void row(List<SheetField> fields, Map<String, Object> values, Set<String> usedKeys) throws IOException {
            float labelBand = LABEL_SIZE + 3;
            float maxWidget = 0;
            for (SheetField f : fields) {
                maxWidget = Math.max(maxWidget, widgetHeight(f));
            }
            float rowHeight = labelBand + maxWidget;
            if (y - rowHeight < MARGIN) {
                newPage();
            }
            float rowTop = y;
            float content = contentWidth();
            int usedCols = 0;
            for (SheetField f : fields) {
                int span = spanOf(f.width());
                float cellX = MARGIN + (usedCols / 12f) * content;
                float cellW = (span / 12f) * content - COL_GAP;
                String key = uniqueKey(f.key(), usedKeys);

                drawText(f.label() == null ? key : f.label(), font, LABEL_SIZE, cellX, rowTop);
                float widgetTop = rowTop - labelBand;
                renderWidget(f, key, cellX, widgetTop, cellW, values.get(f.key()));
                usedCols += span;
            }
            y = rowTop - rowHeight - ROW_GAP;
        }

        private void renderWidget(SheetField f, String key, float x, float top, float w, Object value)
                throws IOException {
            switch (f.type()) {
                case BOOLEAN -> addCheckbox(key, new PDRectangle(x, top - FIELD_H, FIELD_H, FIELD_H),
                        truthy(value));
                case CIRCLES -> addCircles(key, x, top, count(f), filled(value));
                case TEXTAREA -> addTextField(key, new PDRectangle(x, top - TEXTAREA_H, w, TEXTAREA_H),
                        str(value), true);
                default -> addTextField(key, new PDRectangle(x, top - FIELD_H, w, FIELD_H), str(value), false);
            }
        }

        private void addTextField(String key, PDRectangle rect, String value, boolean multiline)
                throws IOException {
            PDTextField field = new PDTextField(form);
            field.setPartialName(key);
            field.setDefaultAppearance("/Helv 10 Tf 0 g");
            if (multiline) {
                field.setMultiline(true);
            }
            PDAnnotationWidget widget = field.getWidgets().get(0);
            widget.setRectangle(rect);
            widget.setPage(page);
            widget.setPrinted(true);
            widget.setAppearanceCharacteristics(new PDAppearanceCharacteristicsDictionary(new COSDictionary()));
            page.getAnnotations().add(widget);
            form.getFields().add(field);
            if (value != null && !value.isBlank()) {
                field.setValue(value);
            }
        }

        private void addCheckbox(String key, PDRectangle rect, boolean checked) throws IOException {
            PDCheckBox box = new PDCheckBox(form);
            box.setPartialName(key);
            PDAnnotationWidget widget = box.getWidgets().get(0);
            widget.setRectangle(rect);
            widget.setPage(page);
            widget.setPrinted(true);
            widget.setAppearanceCharacteristics(new PDAppearanceCharacteristicsDictionary(new COSDictionary()));
            page.getAnnotations().add(widget);
            form.getFields().add(box);
            if (checked) {
                box.check();
            } else {
                box.unCheck();
            }
        }

        /** A row of {@code count} checkbox pips; the first {@code filled} are checked. */
        private void addCircles(String key, float x, float top, int count, int filled) throws IOException {
            for (int i = 0; i < count; i++) {
                float cx = x + i * (CIRCLE + CIRCLE_GAP);
                addCheckbox(key + "_" + (i + 1),
                        new PDRectangle(cx, top - CIRCLE, CIRCLE, CIRCLE), i < filled);
            }
        }

        private void drawText(String text, PDFont f, float size, float x, float yy) throws IOException {
            cs.beginText();
            cs.setFont(f, size);
            cs.newLineAtOffset(x, yy);
            cs.showText(sanitize(text));
            cs.endText();
        }

        private float contentWidth() {
            return page.getMediaBox().getWidth() - 2 * MARGIN;
        }

        void close() throws IOException {
            if (cs != null) {
                cs.close();
            }
        }
    }

    private static int count(SheetField f) {
        return f.count() == null || f.count() < 1 ? 3 : Math.min(f.count(), 20);
    }

    private static int filled(Object value) {
        if (value instanceof Number n) {
            return Math.max(0, n.intValue());
        }
        try {
            return value == null ? 0 : Math.max(0, Integer.parseInt(value.toString().trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Standard-14 Helvetica cannot encode every glyph; drop anything it can't. */
    private String sanitize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            try {
                font.encode(String.valueOf(c));
                sb.append(c);
            } catch (IOException | IllegalArgumentException ex) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    private static boolean truthy(Object value) {
        return value instanceof Boolean b ? b : "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static String str(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
            return String.valueOf(n.longValue());
        }
        return value.toString();
    }
}
