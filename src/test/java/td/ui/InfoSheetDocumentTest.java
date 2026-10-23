package td.ui;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;
import td.term.TermText;
import td.ui.render.IconFace;
import td.ui.render.InfoSheet;
import td.ui.render.SheetIcon;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;
import td.ui.render.SheetLine.Trend;
import td.ui.render.TooltipKey;

import javax.swing.Icon;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.IntStream;

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

    private static StyledDocument layOutLines(SheetLine... lines) {
        FontMetrics metrics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics()
                .getFontMetrics(new Font(Font.DIALOG, Font.PLAIN, 12));
        return InfoSheetDocument.of(new InfoSheet(List.of(lines)), new Java2DFrameRenderer(), 170, metrics);
    }

    @Test
    void aMentionIsItsFaceThenItsTitleInItsInk() {
        StyledDocument doc = layOutLines(SheetLine.Sentence.of(TermText.of("applies ").plus(EffectKind.UNRAVELED)));

        Element face = doc.getCharacterElement(doc.getLength() - " Unravel".length() - 1);
        Element title = doc.getCharacterElement(doc.getLength() - 1);
        Object faceRun = face.getAttributes().getAttribute(HoverKey.INSTANCE);
        Object titleRun = title.getAttributes().getAttribute(HoverKey.INSTANCE);
        assertThat(StyleConstants.getIcon(face.getAttributes())).isNotNull();
        assertThat(faceRun).isInstanceOf(HoverRun.MentionRun.class).isSameAs(titleRun);
        assertThat(StyleConstants.getForeground(title.getAttributes()))
                .isEqualTo(TermIconPainter.ink(TermLooks.faceOf(EffectKind.UNRAVELED).tone()));
    }

    @Test
    void anIconRowIsOneEmbeddedIconAsTallAsItsLayout() {
        IconFace face = TermLooks.faceOf(EffectKind.CHILL);
        List<SheetIcon> icons = IntStream.range(0, 7)
                .mapToObj(i -> new SheetIcon(face, TooltipKey.of("effect", "e" + i), new InfoSheet(List.of()))).toList();

        StyledDocument doc = layOutLines(new SheetLine.IconRow("Debuffs", icons));

        AttributeSet attributes = doc.getCharacterElement(0).getAttributes();
        Icon icon = StyleConstants.getIcon(attributes);
        IconRowLayout layout = IconRowLayout.of(7, false, 166);
        assertThat(attributes.getAttribute(HoverKey.INSTANCE)).isInstanceOf(HoverRun.IconRowRun.class);
        assertThat(icon.getIconHeight()).isEqualTo(layout.height());
    }
}
