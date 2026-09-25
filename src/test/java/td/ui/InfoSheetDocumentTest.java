package td.ui;

import org.junit.jupiter.api.Test;
import td.ui.render.InfoSheet;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;
import td.ui.render.SheetLine.Trend;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InfoSheetDocumentTest {

    private static StyledDocument layOut(Row... rows) {
        FontMetrics metrics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics()
                .getFontMetrics(new Font(Font.DIALOG, Font.PLAIN, 12));
        return InfoSheetDocument.of(new InfoSheet(List.of(rows)), new Java2DFrameRenderer(), 170, metrics);
    }

    private static Color colourOf(StyledDocument doc, String text) throws BadLocationException {
        int at = doc.getText(0, doc.getLength()).indexOf(text);
        return StyleConstants.getForeground(doc.getCharacterElement(at).getAttributes());
    }

    @Test
    void aValueBetterThanItsBaseIsGreenAWorseOnePinkAndAnUnchangedOneTheValueColour() throws BadLocationException {
        StyledDocument doc = layOut(
                Row.plain(Glyph.RING, "Range", "3.2 → 3.7").withTrend(Trend.BETTER),
                Row.plain(Glyph.CHEVRON, "Fire rate", "0.5 → 0.4/s").withTrend(Trend.WORSE),
                Row.plain(Glyph.DOT, "Targets", "first"));

        assertThat(colourOf(doc, "3.2 → 3.7")).isEqualTo(new Color(140, 255, 140));
        assertThat(colourOf(doc, "0.5 → 0.4/s")).isEqualTo(new Color(255, 120, 120));
        assertThat(colourOf(doc, "first")).isEqualTo(new Color(170, 205, 170));
    }
}
