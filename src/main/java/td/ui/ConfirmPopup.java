package td.ui;

import td.ui.render.InfoSheet;
import td.util.ThreadConfined;

import javax.swing.BorderFactory;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.Serial;

/**
 * A question in the HUD look on the frame's popup layer: a sheet of what will happen, a button that does it and
 * Cancel, each showing its key. Enter confirms; Esc, a click anywhere outside it or {@link #cancel()} from its owner
 * cancels. The game behind it keeps running unless the owner pauses it.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public final class ConfirmPopup extends JPanel {

    /** The text colour of a button that can't be undone, as Sell's. */
    public static final Color IRREVERSIBLE = PanelTowerInfo.SELL_TEXT_COLOR;

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Color BACKGROUND = new Color(8, 14, 8);

    private final transient JLayeredPane layer;
    private final transient Runnable onConfirm;
    private final transient Runnable onCancel;
    private final transient AWTEventListener outsideClick = this::outsideClick;
    private final transient KeyEventDispatcher keys = this::keyPressed;
    private boolean open = true;

    private ConfirmPopup(JLayeredPane layer, InfoSheet sheet, String confirm, Color confirmColor, Runnable onConfirm,
                         Runnable onCancel) {
        super(new BorderLayout(0, 6));
        this.layer = layer;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        this.setBackground(BACKGROUND);
        this.setBorder(BorderFactory.createCompoundBorder(Hud.outlineBorder(), new EmptyBorder(6, 8, 6, 8)));
        TooltipPanel content = new TooltipPanel();
        content.setBorder(new EmptyBorder(0, 0, 0, 0));
        content.show(sheet);
        this.add(content, BorderLayout.CENTER);
        HudButton yes = new HudButton(confirm);
        yes.setForeground(confirmColor);
        yes.setFace(ControlFace.button().withChip(GameKey.CONFIRM.chip()));
        yes.addActionListener(event -> this.confirm());
        HudButton no = new HudButton("Cancel");
        no.setFace(ControlFace.button().withChip(GameKey.CANCEL.chip()));
        no.addActionListener(event -> this.cancel());
        JPanel buttons = new JPanel(new BorderLayout(6, 0));
        buttons.setOpaque(false);
        buttons.add(yes, BorderLayout.CENTER);
        buttons.add(no, BorderLayout.EAST);
        this.add(buttons, BorderLayout.SOUTH);
        this.setSize(this.getPreferredSize());
    }

    /**
     * Opens {@code sheet} beside {@code anchor} (in {@code layer}'s coordinates) with a {@code confirm} button
     * written in {@code confirmColor}. {@code onConfirm} runs when it is pressed, {@code onCancel} when the popup
     * is cancelled.
     */
    public static ConfirmPopup open(JLayeredPane layer, Rectangle anchor, InfoSheet sheet, String confirm, Color confirmColor,
                             Runnable onConfirm, Runnable onCancel) {
        ConfirmPopup popup = new ConfirmPopup(layer, sheet, confirm, confirmColor, onConfirm, onCancel);
        layer.add(popup, JLayeredPane.POPUP_LAYER, 0);
        popup.setLocation(TooltipPlacement.place(anchor, popup.getSize(),
                new Rectangle(0, 0, layer.getWidth(), layer.getHeight())));
        popup.validate();
        Toolkit.getDefaultToolkit().addAWTEventListener(popup.outsideClick, AWTEvent.MOUSE_EVENT_MASK);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(popup.keys);
        layer.repaint();
        return popup;
    }

    public boolean isOpen() {
        return this.open;
    }

    /** Closes it and does what it asked, as its button would. */
    public void confirm() {
        if (this.open) {
            this.close();
            this.onConfirm.run();
        }
    }

    /** Closes it as Cancel would. */
    public void cancel() {
        if (this.open) {
            this.close();
            this.onCancel.run();
        }
    }

    private void close() {
        this.open = false;
        Toolkit.getDefaultToolkit().removeAWTEventListener(this.outsideClick);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this.keys);
        this.layer.remove(this);
        this.layer.repaint();
    }

    /** Listens to mouse events only, so every event is one. */
    private void outsideClick(AWTEvent event) {
        MouseEvent mouse = (MouseEvent) event;
        if (mouse.getID() == MouseEvent.MOUSE_PRESSED && !SwingUtilities.isDescendingFrom(mouse.getComponent(), this)) {
            this.cancel();
        }
    }

    /** Enter confirms and Esc cancels, and neither goes further: they mean nothing else while a question is open. */
    private boolean keyPressed(KeyEvent event) {
        boolean enter = event.getKeyCode() == GameKey.CONFIRM.keyCode() || event.getKeyChar() == '\n';
        boolean esc = event.getKeyCode() == GameKey.CANCEL.keyCode() || event.getKeyChar() == KeyEvent.VK_ESCAPE;
        if (event.getID() == KeyEvent.KEY_PRESSED) {
            if (enter) {
                this.confirm();
            } else if (esc) {
                this.cancel();
            }
        }
        return enter || esc;
    }
}
