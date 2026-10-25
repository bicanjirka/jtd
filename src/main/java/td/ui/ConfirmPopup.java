package td.ui;

import td.ui.render.InfoSheet;
import td.util.ThreadConfined;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
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
 * Cancel. Esc, a click anywhere outside it or {@link #cancel()} from its owner cancels; the game behind it keeps
 * running unless the owner pauses it.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public final class ConfirmPopup extends JPanel {

    /** The text colour of a button that can't be undone, as Sell's. */
    public static final Color IRREVERSIBLE = PanelTowerInfo.SELL_TEXT_COLOR;

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Color BACKGROUND = new Color(8, 14, 8);
    private static final Color HINT = Hud.TEXT_DIM;
    private static final Font HINT_FONT = Hud.LABEL_FONT.deriveFont(9f);

    private final transient JLayeredPane layer;
    private final transient Runnable onCancel;
    private final transient AWTEventListener outsideClick = this::outsideClick;
    private final transient KeyEventDispatcher escape = this::escape;
    private boolean open = true;

    private ConfirmPopup(JLayeredPane layer, InfoSheet sheet, String confirm, Color confirmColor, Runnable onConfirm,
                         Runnable onCancel) {
        super(new BorderLayout(0, 6));
        this.layer = layer;
        this.onCancel = onCancel;
        this.setBackground(BACKGROUND);
        this.setBorder(BorderFactory.createCompoundBorder(Hud.outlineBorder(), new EmptyBorder(6, 8, 6, 8)));
        TooltipPanel content = new TooltipPanel();
        content.setBorder(new EmptyBorder(0, 0, 0, 0));
        content.show(sheet);
        this.add(content, BorderLayout.CENTER);
        HudButton yes = new HudButton(confirm);
        yes.setForeground(confirmColor);
        yes.addActionListener(event -> {
            this.close();
            onConfirm.run();
        });
        HudButton no = new HudButton("Cancel");
        no.addActionListener(event -> this.cancel());
        JPanel buttons = new JPanel(new BorderLayout(6, 0));
        buttons.setOpaque(false);
        buttons.add(yes, BorderLayout.CENTER);
        buttons.add(no, BorderLayout.EAST);
        JLabel hint = new JLabel("Esc or a click elsewhere cancels");
        hint.setFont(HINT_FONT);
        hint.setForeground(HINT);
        JPanel south = new JPanel(new BorderLayout(0, 3));
        south.setOpaque(false);
        south.add(buttons, BorderLayout.CENTER);
        south.add(hint, BorderLayout.SOUTH);
        this.add(south, BorderLayout.SOUTH);
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
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(popup.escape);
        layer.repaint();
        return popup;
    }

    public boolean isOpen() {
        return this.open;
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
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this.escape);
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

    /** Esc cancels, and goes no further: it means nothing else while a question is open. */
    private boolean escape(KeyEvent event) {
        boolean esc = event.getKeyCode() == KeyEvent.VK_ESCAPE || event.getKeyChar() == KeyEvent.VK_ESCAPE;
        if (esc && event.getID() == KeyEvent.KEY_PRESSED) {
            this.cancel();
        }
        return esc;
    }
}
