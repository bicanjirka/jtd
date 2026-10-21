package td.ui;

import td.ui.render.InfoSheet;
import td.ui.render.SheetTooltip;
import td.ui.render.TooltipKey;
import td.util.ThreadConfined;

import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The game's tooltips on the frame's popup layer. Opens one where the pointer rests on a target, chains one from a
 * target inside a tooltip, keeps them while the pointer is over a source or a tooltip and closes them all a moment
 * after it leaves; the pointer coming back to a source closes what that source opened. An open tooltip follows its
 * key through new sheets.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public final class TooltipLayer {

    static final int OPEN_DELAY_MS = 150;
    static final int CLOSE_DELAY_MS = 250;
    private static final String TERM_KIND = "term:";

    private final JLayeredPane layer;
    private final Timer openTimer = new Timer(OPEN_DELAY_MS, event -> this.openPending());
    private final Timer closeTimer = new Timer(CLOSE_DELAY_MS, event -> this.closeAll());
    private final Timer childCloseTimer = new Timer(CLOSE_DELAY_MS, event -> this.closeIdleChildren());
    private final Map<SheetPane, Integer> paneDepths = new IdentityHashMap<>();
    private final Map<Component, Supplier<Optional<SheetTooltip>>> buttons = new IdentityHashMap<>();
    private final List<Open> opened = new ArrayList<>();
    private TooltipChain chain = TooltipChain.empty();
    private Pending pending;
    private int idleDepth;

    /** What a tooltip is about and where it opens from: a target in a pane, or a button. */
    private record Pending(int depth, SheetTooltip tooltip, Component source, Rectangle anchor) {
    }

    /** An open tooltip: its panel, the depth of its source, and where it came from. */
    private record Open(int depth, TooltipPanel panel, TooltipKey key, Component source) {
    }

    public TooltipLayer(JLayeredPane layer) {
        this.layer = layer;
        this.openTimer.setRepeats(false);
        this.closeTimer.setRepeats(false);
        this.childCloseTimer.setRepeats(false);
    }

    /** A pane whose sheet can change; it is the depth-0 source of the tooltips its targets open. */
    void watch(SheetPane pane) {
        this.watch(pane, 0);
    }

    private void watch(SheetPane pane, int depth) {
        if (this.paneDepths.putIfAbsent(pane, depth) != null) {
            return;
        }
        pane.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                TooltipLayer.this.paneMoved(pane, event);
            }
        });
        pane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                pane.clearPointer();
                TooltipLayer.this.startClose();
            }

            @Override
            public void mouseEntered(MouseEvent event) {
                TooltipLayer.this.enteredAt(depth);
            }
        });
    }

    /** The pointer is back on a source at {@code depth}: what it opened is no longer wanted. */
    private void enteredAt(int depth) {
        this.closeTimer.stop();
        this.childCloseTimer.stop();
        this.pending = null;
        this.closeFrom(depth + 1);
    }

    /**
     * A button, a depth-0 source, whose hover opens the tooltip {@code tooltip} gives for it now, if any. The
     * tooltip belongs to the button: it follows whatever the button offers.
     */
    void watchButton(HudButton button, Supplier<Optional<SheetTooltip>> tooltip) {
        this.buttons.put(button, tooltip);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                TooltipLayer.this.closeTimer.stop();
                TooltipLayer.this.buttonEntered(button);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                TooltipLayer.this.openTimer.stop();
                TooltipLayer.this.startClose();
            }
        });
    }

    /** The watched buttons' offers may have changed: an open button tooltip shows its button's current one. */
    void refreshButtons() {
        for (Open open : new ArrayList<>(this.opened)) {
            Supplier<Optional<SheetTooltip>> offer = this.buttons.get(open.source());
            if (offer == null) {
                continue;
            }
            Optional<SheetTooltip> now = offer.get();
            if (now.isPresent()) {
                open.panel().show(now.get().sheet());
            } else {
                this.closeFrom(open.depth());
            }
        }
    }

    /** {@code source} now shows {@code sheet}: a tooltip it opened takes its key's new content, or shows Expired. */
    void sheetChanged(SheetPane source, InfoSheet sheet) {
        for (Open open : new ArrayList<>(this.opened)) {
            Optional<InfoSheet> from = this.sheetOf(open.source(), source, sheet);
            if (from.isEmpty() || open.key().id().startsWith(TERM_KIND)) {
                continue;
            }
            Optional<InfoSheet> next = from.get().tooltipFor(open.key());
            if (next.isPresent()) {
                open.panel().show(next.get());
            } else {
                open.panel().showExpired();
            }
        }
    }

    public void closeAll() {
        this.openTimer.stop();
        this.closeTimer.stop();
        this.pending = null;
        this.paneDepths.keySet().forEach(pane -> pane.highlight(Optional.empty()));
        this.closeFrom(0);
    }

    private Optional<InfoSheet> sheetOf(Component openedFrom, SheetPane changed, InfoSheet sheet) {
        if (openedFrom == changed) {
            return Optional.of(sheet);
        }
        return this.opened.stream().filter(open -> open.panel().pane() == openedFrom).findFirst()
                .map(open -> open.panel().sheet());
    }

    private void paneMoved(SheetPane pane, MouseEvent event) {
        Optional<HoverTarget> target = pane.hoverAt(event.getPoint());
        pane.highlight(target);
        this.watchChild(this.paneDepths.get(pane) + 1, target);
        if (target.isEmpty()) {
            this.openTimer.stop();
            this.pending = null;
            return;
        }
        HoverTarget hovered = target.get();
        if (this.pending != null && this.pending.source() == pane && this.pending.tooltip().key().equals(hovered.key())) {
            return;
        }
        Rectangle anchor = SwingUtilities.convertRectangle(pane, hovered.bounds(), this.layer);
        this.pending = new Pending(this.paneDepths.get(pane), new SheetTooltip(hovered.key(), hovered.sheet()), pane, anchor);
        this.openTimer.restart();
    }

    /**
     * The pointer is on a source whose tooltip is open at {@code depth}: while it is on what opened it, the tooltip
     * stays; off it, the tooltip closes a moment later unless the pointer reaches it or comes back.
     */
    private void watchChild(int depth, Optional<HoverTarget> target) {
        boolean open = this.opened.stream().anyMatch(shown -> shown.depth() == depth);
        boolean onIt = target.isPresent() && this.opened.stream()
                .anyMatch(shown -> shown.depth() == depth && shown.key().equals(target.get().key()));
        if (!open || onIt) {
            this.childCloseTimer.stop();
        } else if (!this.childCloseTimer.isRunning()) {
            this.idleDepth = depth;
            this.childCloseTimer.start();
        }
    }

    /** The pointer stayed off what opened the tooltips at {@code idleDepth}: they and what they chained close. */
    void closeIdleChildren() {
        this.closeFrom(this.idleDepth);
    }

    private void buttonEntered(HudButton button) {
        Optional<SheetTooltip> offer = this.buttons.get(button).get();
        if (offer.isEmpty()) {
            return;
        }
        Rectangle anchor = SwingUtilities.convertRectangle(button.getParent(), button.getBounds(), this.layer);
        this.pending = new Pending(0, offer.get(), button, anchor);
        this.openTimer.restart();
    }

    void openPending() {
        Pending request = this.pending;
        if (request == null) {
            return;
        }
        Optional<TooltipChain> next = this.chain.open(request.depth(), request.tooltip().key());
        if (next.isEmpty()) {
            return;
        }
        this.closeFrom(request.depth() + 1);
        this.chain = next.get();
        TooltipPanel panel = new TooltipPanel();
        panel.show(request.tooltip().sheet());
        this.layer.add(panel, JLayeredPane.POPUP_LAYER);
        panel.setLocation(TooltipPlacement.place(request.anchor(), panel.getSize(),
                new Rectangle(0, 0, this.layer.getWidth(), this.layer.getHeight())));
        this.watch(panel.pane(), request.depth() + 1);
        MouseAdapter keepOpen = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                TooltipLayer.this.enteredAt(request.depth() + 1);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                TooltipLayer.this.startClose();
            }
        };
        panel.addMouseListener(keepOpen);
        this.opened.add(new Open(request.depth() + 1, panel, request.tooltip().key(), request.source()));
        this.layer.repaint();
    }

    /** Closes the tooltips at {@code depth} and deeper. */
    private void closeFrom(int depth) {
        for (Open open : new ArrayList<>(this.opened)) {
            if (open.depth() >= depth) {
                this.layer.remove(open.panel());
                this.opened.remove(open);
                this.paneDepths.remove(open.panel().pane());
            }
        }
        this.chain = this.chain.closedFrom(Math.max(0, depth - 1));
        this.layer.repaint();
    }

    private void startClose() {
        if (!this.opened.isEmpty()) {
            this.closeTimer.restart();
        }
    }
}
