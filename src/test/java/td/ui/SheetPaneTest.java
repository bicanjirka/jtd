package td.ui;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;
import td.term.TermText;
import td.ui.render.InfoSheet;
import td.ui.render.SheetIcon;
import td.ui.render.SheetLine;
import td.ui.render.TooltipKey;

import javax.swing.text.BadLocationException;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SheetPaneTest {

    private static final int WIDTH = 184;

    private static SheetPane paneWith(SheetLine... lines) {
        SheetPane pane = new SheetPane();
        pane.setSize(WIDTH, 200);
        pane.show(new InfoSheet(List.of(lines)), new Java2DFrameRenderer(), WIDTH);
        pane.setSize(WIDTH, 200);
        return pane;
    }

    @Test
    void hoverAtFindsTheMentionUnderThePoint() throws BadLocationException {
        SheetPane pane = paneWith(SheetLine.Sentence.of(TermText.of("applies ").plus(EffectKind.UNRAVELED)));
        int title = pane.getDocument().getText(0, pane.getDocument().getLength()).indexOf("Unravel");
        Rectangle box = pane.modelToView2D(title + 2).getBounds();

        Optional<HoverTarget> target = pane.hoverAt(new Point(box.x, box.y + box.height / 2));

        assertThat(target).isPresent();
        assertThat(target.get()).isInstanceOfSatisfying(HoverTarget.Mention.class,
                mention -> assertThat(mention.mention().term()).isEqualTo(EffectKind.UNRAVELED));
    }

    @Test
    void aPointOnPlainTextHitsNothing() {
        SheetPane pane = paneWith(SheetLine.Sentence.of(TermText.of("applies ").plus(EffectKind.UNRAVELED)));

        assertThat(pane.hoverAt(new Point(2, 6))).isEmpty();
    }

    @Test
    void everyIconOfARowIsHoverableNotOnlyThoseInTheLeftHalf() throws BadLocationException {
        List<SheetIcon> icons = Stream.of(EffectKind.CHILL, EffectKind.BURN, EffectKind.POISON, EffectKind.FREEZE,
                        EffectKind.UNRAVELED)
                .map(kind -> new SheetIcon(TermLooks.faceOf(kind), TooltipKey.ofTerm("applies", kind),
                        new InfoSheet(List.of()))).toList();
        SheetLine.IconRow row = new SheetLine.IconRow("Applies", icons);
        SheetPane pane = paneWith(row);
        IconRowLayout layout = IconRowLayout.of(icons.size(), false, WIDTH);
        Rectangle first = pane.modelToView2D(0).getBounds();

        for (int i = 0; i < icons.size(); i++) {
            Point cell = layout.cell(i);
            Point centre = new Point(first.x + cell.x + TermIconPainter.ROW / 2, first.y + cell.y + TermIconPainter.ROW / 2);

            assertThat(pane.hoverAt(centre)).as("icon %d", i).isPresent();
            assertThat(pane.hoverAt(centre).get().key()).isEqualTo(icons.get(i).key());
        }
    }
}
