package td.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import td.economy.StatusTerm;
import td.effect.EffectKind;
import td.term.TermText;
import td.ui.render.InfoSheet;
import td.ui.render.SheetIcon;
import td.ui.render.SheetLine;
import td.ui.render.SheetTooltip;
import td.ui.render.TooltipKey;

import javax.swing.JLayeredPane;
import javax.swing.text.BadLocationException;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EventDispatchThreadExtension.class)
class TooltipLayerTest {

    private static final int WIDTH = 184;
    /** The tests open and close tooltips themselves, so no timer may do it for them, during a test or after. */
    private static final TooltipDelays NEVER = new TooltipDelays(Integer.MAX_VALUE, Integer.MAX_VALUE);

    private final JLayeredPane layer = new JLayeredPane();
    private final TooltipLayer tooltips = new TooltipLayer(this.layer, NEVER);

    TooltipLayerTest() {
        this.layer.setSize(800, 600);
    }

    private static InfoSheet tooltipWithAMention() {
        return new InfoSheet(List.of(SheetLine.Sentence.of(TermText.of("applies ").plus(EffectKind.UNRAVELED))));
    }

    private SheetPane paneWithOneIcon() {
        SheetIcon icon = new SheetIcon(TermLooks.faceOf(EffectKind.CHILL), TooltipKey.ofTerm("applies", EffectKind.CHILL),
                tooltipWithAMention());
        SheetPane pane = new SheetPane();
        pane.setSize(WIDTH, 200);
        pane.show(new InfoSheet(List.of(new SheetLine.IconRow("Applies", List.of(icon)))), new Java2DFrameRenderer(), WIDTH);
        pane.setSize(WIDTH, 200);
        this.layer.add(pane);
        pane.setLocation(600, 100);
        this.tooltips.watch(pane);
        return pane;
    }

    private static void move(Component source, Point at) {
        source.dispatchEvent(new MouseEvent(source, MouseEvent.MOUSE_MOVED, 0, 0, at.x, at.y, 0, false));
    }

    private static void enter(Component source) {
        source.dispatchEvent(new MouseEvent(source, MouseEvent.MOUSE_ENTERED, 0, 0, 1, 1, 0, false));
    }

    private List<Component> popups() {
        return List.of(this.layer.getComponentsInLayer(JLayeredPane.POPUP_LAYER));
    }

    private static Point firstIconCentre(SheetPane pane) throws BadLocationException {
        Rectangle first = pane.modelToView2D(0).getBounds();
        return new Point(first.x + IconRowLayout.LABEL_WIDTH + TermIconPainter.ROW / 2, first.y + TermIconPainter.ROW / 2);
    }

    /** Where the pane's first hoverable thing is, found by looking, since a pane no window shows is not laid out. */
    private static Point hoverablePoint(SheetPane pane) {
        for (int y = 0; y < pane.getHeight(); y++) {
            for (int x = 0; x < pane.getWidth(); x++) {
                if (pane.hoverAt(new Point(x, y)).isPresent()) {
                    return new Point(x, y);
                }
            }
        }
        throw new IllegalStateException("Nothing to hover in " + pane.getText());
    }

    @Test
    void thePointerComingBackToTheSourceClosesWhatItOpened() throws BadLocationException {
        SheetPane pane = this.paneWithOneIcon();
        move(pane, firstIconCentre(pane));
        this.tooltips.openPending();
        assertThat(this.popups()).hasSize(1);

        enter(pane);

        assertThat(this.popups()).isEmpty();
    }

    @Test
    void aChainedTooltipIsDrawnOverTheOneItCameFrom() throws BadLocationException {
        SheetPane pane = this.paneWithOneIcon();
        move(pane, firstIconCentre(pane));
        this.tooltips.openPending();
        TooltipPanel first = (TooltipPanel) this.popups().getFirst();
        SheetPane inner = first.pane();
        inner.setSize(inner.getWidth(), inner.getPreferredSize().height);

        move(inner, hoverablePoint(inner));
        this.tooltips.openPending();

        Component chained = this.popups().stream().filter(popup -> popup != first).findFirst().orElseThrow();
        assertThat(this.layer.getPosition(chained)).isLessThan(this.layer.getPosition(first));
    }

    @Test
    void thePointerComingBackToATooltipClosesTheOneItChained() throws BadLocationException {
        SheetPane pane = this.paneWithOneIcon();
        move(pane, firstIconCentre(pane));
        this.tooltips.openPending();
        TooltipPanel first = (TooltipPanel) this.popups().getFirst();
        SheetPane inner = first.pane();
        inner.setSize(inner.getWidth(), inner.getPreferredSize().height);
        move(inner, hoverablePoint(inner));
        this.tooltips.openPending();
        assertThat(this.popups()).hasSize(2);

        enter(inner);

        assertThat(this.popups()).containsExactly(first);
    }

    @Test
    void thePointerLeavingWhatOpenedATooltipForOtherPartsOfItsSourceClosesItAfterTheGrace() throws BadLocationException {
        SheetPane pane = this.paneWithOneIcon();
        move(pane, firstIconCentre(pane));
        this.tooltips.openPending();

        move(pane, new Point(2, 2));
        this.tooltips.closeIdleChildren();

        assertThat(this.popups()).isEmpty();
    }

    @Test
    void thePointerLeavingTheMentionForOtherTextOfItsTooltipClosesTheChainedOne() throws BadLocationException {
        SheetPane pane = this.paneWithOneIcon();
        move(pane, firstIconCentre(pane));
        this.tooltips.openPending();
        TooltipPanel first = (TooltipPanel) this.popups().getFirst();
        SheetPane inner = first.pane();
        inner.setSize(inner.getWidth(), inner.getPreferredSize().height);
        move(inner, hoverablePoint(inner));
        this.tooltips.openPending();
        assertThat(this.popups()).hasSize(2);

        move(inner, new Point(1, 1));
        this.tooltips.closeIdleChildren();

        assertThat(this.popups()).containsExactly(first);
    }

    @Test
    void aSourceInTheTopStripOpensItsTooltipUnderItWithLeftEdgesAligned() {
        StatusIcon icon = new StatusIcon(StatusTerm.CASH);
        icon.setBounds(300, 4, 90, 28);
        this.layer.add(icon);
        this.tooltips.watchBelow(icon, () -> Optional.of(new SheetTooltip(
                TooltipKey.ofTerm("status", StatusTerm.CASH),
                StatusSheetText.of(StatusTerm.CASH, StatusReadout.empty(), RoundStrip.empty()))));

        icon.dispatchEvent(new MouseEvent(icon, MouseEvent.MOUSE_ENTERED, 0, 0, 1, 1, 0, false));
        this.tooltips.openPending();

        Component popup = this.popups().getFirst();
        assertThat(popup.getX()).isEqualTo(300);
        assertThat(popup.getY()).isEqualTo(4 + 28 + TooltipPlacement.GAP);
    }
}
