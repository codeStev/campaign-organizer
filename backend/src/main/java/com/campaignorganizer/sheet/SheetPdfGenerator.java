package com.campaignorganizer.sheet;

import com.campaignorganizer.sheet.SheetSchema.SheetField;
import com.campaignorganizer.sheet.SheetSchema.SheetSection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * Builds a fillable AcroForm PDF from an arbitrary sheet template (ADR-0029),
 * used for systems without a bundled official sheet. The output stays fillable.
 */
@Service
public class SheetPdfGenerator {

    private static final float MARGIN = 50;
    private static final float LABEL_SIZE = 9;
    private static final float FIELD_H = 16;
    private static final float TEXTAREA_H = 54;
    private static final float LINE_GAP = 6;

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
                for (SheetField field : section.fields() == null ? List.<SheetField>of() : field(section)) {
                    layout.field(field, uniqueKey(field.key(), usedKeys), v.get(field.key()));
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

    private static List<SheetField> field(SheetSection section) {
        return section.fields();
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

    /** Cursor-based single-column layout that paginates as it fills the page. */
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

        private void ensure(float needed) throws IOException {
            if (y - needed < MARGIN) {
                newPage();
            }
        }

        void title(String text) throws IOException {
            drawText(text, bold, 16, MARGIN, y);
            y -= 26;
        }

        void heading(String text) throws IOException {
            ensure(24);
            y -= 6;
            drawText(text == null ? "" : text, bold, 12, MARGIN, y);
            float width = contentWidth();
            cs.moveTo(MARGIN, y - 4);
            cs.lineTo(MARGIN + width, y - 4);
            cs.stroke();
            y -= 18;
        }

        void field(SheetField f, String key, Object value) throws IOException {
            boolean area = f.type() == SheetSchema.FieldType.TEXTAREA;
            boolean check = f.type() == SheetSchema.FieldType.BOOLEAN;
            float boxH = area ? TEXTAREA_H : FIELD_H;
            ensure(LABEL_SIZE + 2 + boxH + LINE_GAP);

            drawText(f.label() == null ? key : f.label(), font, LABEL_SIZE, MARGIN, y);
            y -= (LABEL_SIZE + 3);

            float width = check ? FIELD_H : contentWidth();
            PDRectangle rect = new PDRectangle(MARGIN, y - boxH, width, boxH);
            if (check) {
                addCheckbox(key, rect, truthy(value));
            } else {
                addTextField(key, rect, str(value), area);
            }
            y -= (boxH + LINE_GAP);
        }

        private void addTextField(String key, PDRectangle rect, String value, boolean multiline)
                throws IOException {
            PDTextField field = new PDTextField(form);
            field.setPartialName(key);
            field.setDefaultAppearance("/Helv 10 Tf 0 g");
            if (multiline) {
                field.setMultiline(true);
            }
            attachWidget(field, rect);
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
            PDAppearanceCharacteristicsDictionary appearance =
                    new PDAppearanceCharacteristicsDictionary(new org.apache.pdfbox.cos.COSDictionary());
            widget.setAppearanceCharacteristics(appearance);
            widget.setPrinted(true);
            page.getAnnotations().add(widget);
            form.getFields().add(box);
            if (checked) {
                box.check();
            } else {
                box.unCheck();
            }
        }

        private void attachWidget(PDTextField field, PDRectangle rect) throws IOException {
            PDAnnotationWidget widget = field.getWidgets().get(0);
            widget.setRectangle(rect);
            widget.setPage(page);
            widget.setPrinted(true);
            // Light border so the field is visible.
            var border = new PDAppearanceCharacteristicsDictionary(new org.apache.pdfbox.cos.COSDictionary());
            widget.setAppearanceCharacteristics(border);
            page.getAnnotations().add(widget);
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
