package td.ui;

import td.util.ThreadConfined;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicTextFieldUI;

import java.awt.Color;
import java.io.Serial;

/**
 * A text input in the HUD look. Swing's cross-platform basic delegate paints it, never the
 * platform look-and-feel, so it looks the same on every OS.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class HudTextField extends JTextField {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Color FIELD_BACKGROUND = new Color(14, 22, 14);
    private static final Color SELECTION = new Color(46, 66, 46);

    HudTextField(String text, int columns) {
        super(text, columns);
        setUI(new BasicTextFieldUI());
        setFont(Hud.LABEL_FONT);
        setBackground(FIELD_BACKGROUND);
        setForeground(Hud.FOREGROUND);
        setCaretColor(Hud.FOREGROUND);
        setSelectionColor(SELECTION);
        setSelectedTextColor(Hud.FOREGROUND);
        setBorder(BorderFactory.createCompoundBorder(Hud.outlineBorder(), BorderFactory.createEmptyBorder(2, 4, 2, 4)));
    }
}
