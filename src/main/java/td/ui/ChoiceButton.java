package td.ui;

import td.util.ThreadConfined;
import td.wave.WaveScriptSuggestions.Candidate;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.List;
import java.util.Locale;

/**
 * A button showing the chosen item's icon and title in its colours; a click opens the list of every item, all of it
 * in view. The list takes the keys while it is open: arrows, Page Up/Down, Home and End move, a letter jumps to the
 * next title it starts, Enter or Space takes the row and Esc closes it.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class ChoiceButton<T> {

    private static final String OPENS = "▾";
    /** Room besides the title and the icon: the paddings, the icon's gap and the gap before the arrow. */
    private static final int CHROME = 6 + 4 + 10 + 6;

    private final HudButton button = new HudButton("");
    private final Choices<T> choices;
    private final SuggestionList list;
    private final Runnable onClosed;

    private T chosen;

    /** {@code onClosed} runs when Enter, Space, Esc or a click closes the list, so the keys can go elsewhere. */
    ChoiceButton(Choices<T> choices, T initial, Runnable onClosed) {
        this.choices = choices;
        this.onClosed = onClosed;
        this.list = new SuggestionList(choices::lookOfRow, this::take);
        this.list.setFocusable(true);
        this.list.setFocusTraversalKeysEnabled(false);
        this.bind(KeyEvent.VK_UP, () -> this.list.move(-1));
        this.bind(KeyEvent.VK_DOWN, () -> this.list.move(1));
        this.bind(KeyEvent.VK_LEFT, () -> this.list.moveAcross(-1));
        this.bind(KeyEvent.VK_RIGHT, () -> this.list.moveAcross(1));
        this.bind(KeyEvent.VK_PAGE_UP, () -> this.list.select(Math.max(0, this.list.selectedIndex() - this.list.visibleRows())));
        this.bind(KeyEvent.VK_PAGE_DOWN, () -> this.list.select(
                Math.min(this.list.rowCount() - 1, this.list.selectedIndex() + this.list.visibleRows())));
        this.bind(KeyEvent.VK_HOME, () -> this.list.select(0));
        this.bind(KeyEvent.VK_END, () -> this.list.select(this.list.rowCount() - 1));
        this.bind(KeyEvent.VK_ENTER, this::take);
        this.bind(KeyEvent.VK_SPACE, this::take);
        this.bind(KeyEvent.VK_ESCAPE, () -> {
            this.close();
            this.onClosed.run();
        });
        this.list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (Character.isLetterOrDigit(e.getKeyChar())) {
                    ChoiceButton.this.jumpTo(e.getKeyChar());
                }
            }
        });
        this.list.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                ChoiceButton.this.close();
            }
        });
        this.button.addActionListener(evt -> this.toggle());
        this.button.setPreferredSize(this.size());
        this.show(initial);
    }

    JComponent component() {
        return this.button;
    }

    T chosen() {
        return this.chosen;
    }

    /** Whether the open list has the keys. */
    boolean holdsFocus() {
        return this.list.isFocusOwner();
    }

    void toggle() {
        if (this.list.isHung()) {
            this.close();
            this.onClosed.run();
            return;
        }
        this.list.show(this.choices.rows());
        this.list.hangInColumnsBy(this.button);
        this.list.select(Math.max(0, this.choices.indexOf(this.chosen)));
        this.list.requestFocusInWindow();
    }

    private void take() {
        this.show(this.choices.itemAt(this.list.selectedIndex()));
        this.close();
        this.onClosed.run();
    }

    private void close() {
        this.list.takeDown();
    }

    /** Highlights the next row after the highlighted one whose title starts with {@code key}, wrapping round. */
    private void jumpTo(char key) {
        List<Candidate> rows = this.choices.rows();
        String start = String.valueOf(key).toLowerCase(Locale.ROOT);
        for (int step = 1; step <= rows.size(); step++) {
            int index = (this.list.selectedIndex() + step) % rows.size();
            if (rows.get(index).text().toLowerCase(Locale.ROOT).startsWith(start)) {
                this.list.select(index);
                return;
            }
        }
    }

    private void show(T item) {
        this.chosen = item;
        RowLook look = this.choices.lookOf(item);
        this.button.setText(this.choices.rowOf(item).text() + "\t" + OPENS);
        this.button.setForeground(look.text());
        this.button.setIcon(look.icon());
    }

    /** Wide enough for the longest title, so choosing never shifts the controls after it. */
    private Dimension size() {
        FontMetrics metrics = this.button.getFontMetrics(Hud.LABEL_FONT);
        int widest = this.choices.rows().stream().mapToInt(row -> metrics.stringWidth(row.text())).max().orElse(0);
        int icon = this.choices.lookOf(this.choices.itemAt(0)).icon().getIconWidth();
        return new Dimension(CHROME + icon + widest + metrics.stringWidth(OPENS),
                new HudButton("X").getPreferredSize().height);
    }

    private void bind(int keyCode, Runnable action) {
        String name = "choose-" + keyCode;
        this.list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(keyCode, 0), name);
        this.list.getActionMap().put(name, new AbstractAction() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent evt) {
                action.run();
            }
        });
    }
}
