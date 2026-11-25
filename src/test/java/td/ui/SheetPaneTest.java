package td.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

@ExtendWith(EventDispatchThreadExtension.class)
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
    void aWrappedMentionIsHitOnlyOnItsOwnTextOnEachLine() throws BadLocationException {
        SheetPane pane = wrappedMentionPane();
        String text = pane.getDocument().getText(0, pane.getDocument().getLength());
        Rectangle title = pane.modelToView2D(text.indexOf("Unravel") + 2).getBounds();
        Rectangle face = pane.modelToView2D(text.indexOf("Unravel") - 2).getBounds();
        Rectangle lineStart = pane.modelToView2D(0).getBounds();

        Optional<HoverTarget> onTitle = pane.hoverAt(new Point(title.x, title.y + title.height / 2));
        Optional<HoverTarget> onFace = pane.hoverAt(new Point(face.x + 2, face.y + face.height / 2));
        Optional<HoverTarget> beforeIt = pane.hoverAt(new Point(lineStart.x + 2, face.y + face.height / 2));

        assertThat(onTitle).isPresent();
        assertThat(onFace).isPresent();
        assertThat(onFace.get().key()).isEqualTo(onTitle.get().key());
        assertThat(beforeIt).isEmpty();
    }

    @Test
    void aWrappedMentionLightsEachLineItCoversApart() throws BadLocationException {
        SheetPane pane = wrappedMentionPane();
        String text = pane.getDocument().getText(0, pane.getDocument().getLength());
        Rectangle title = pane.modelToView2D(text.indexOf("Unravel") + 2).getBounds();

        List<Rectangle> areas = pane.hoverAt(new Point(title.x, title.y + title.height / 2)).orElseThrow().areas();

        assertThat(areas).hasSize(2);
        assertThat(areas.get(0).y).isLessThan(areas.get(1).y);
        assertThat(areas).noneMatch(area -> area.width >= WIDTH / 2);
    }

    /** A sentence whose mention starts at the end of its first line and wraps its title onto the second. */
    private static SheetPane wrappedMentionPane() throws BadLocationException {
        for (int words = 1; words < 40; words++) {
            SheetPane pane = paneWith(SheetLine.Sentence.of(TermText.of("ab ".repeat(words)).plus(EffectKind.UNRAVELED)
                    .plus(" tail")));
            String text = pane.getDocument().getText(0, pane.getDocument().getLength());
            int title = text.indexOf("Unravel");
            if (pane.modelToView2D(title).getY() > pane.modelToView2D(title - 2).getY()) {
                return pane;
            }
        }
        throw new IllegalStateException("No prefix wraps the mention");
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
