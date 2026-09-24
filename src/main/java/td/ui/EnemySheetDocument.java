package td.ui;

import td.ui.render.EnemySheet;
import td.ui.render.SheetLine;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;

/**
 * Lays an {@link EnemySheet} out as a styled document: one paragraph per line, values on a
 * right-aligned tab stop at the pane's edge.
 */
final class EnemySheetDocument {

    private EnemySheetDocument() {
    }

    static StyledDocument of(EnemySheet sheet, int width) {
        DefaultStyledDocument doc = new DefaultStyledDocument();
        SimpleAttributeSet paragraph = new SimpleAttributeSet();
        StyleConstants.setTabSet(paragraph, new TabSet(new TabStop[]{new TabStop(width, TabStop.ALIGN_RIGHT, TabStop.LEAD_NONE)}));
        try {
            for (SheetLine line : sheet.lines()) {
                String text = switch (line) {
                    case SheetLine.Header header -> header.name() + "\t" + header.rank();
                    case SheetLine.HealthBar bar -> bar.health() + " / " + bar.maxHealth() + "\t" + bar.value();
                    case SheetLine.Row row -> row.label() + "\t" + row.value();
                    case SheetLine.Prose prose -> prose.text();
                    case SheetLine.Gap gap -> "";
                };
                doc.insertString(doc.getLength(), (doc.getLength() == 0 ? "" : "\n") + text, null);
            }
        } catch (BadLocationException e) {
            throw new IllegalStateException("Appending to the end of a fresh document", e);
        }
        doc.setParagraphAttributes(0, doc.getLength(), paragraph, false);
        return doc;
    }
}
