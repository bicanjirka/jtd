package td.ui;

import td.util.ThreadConfined;
import td.wave.WaveScriptSuggestions.Candidate;
import td.wave.WaveScriptSuggestions.Result;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

/**
 * Suggests the next token of a field's wave script. The best match shows as ghost text that Tab or
 * Right accepts; Down opens the list of every match, and Enter takes the highlighted one. Esc closes
 * the list before it leaves the field, Enter with the list closed applies the field as ever, and Tab
 * with nothing typed moves focus on.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class WaveScriptCompleter {

    private final HudTextField field;
    private final BiFunction<String, Integer, Result> source;
    private final SuggestionList list = new SuggestionList();

    private Result result = Result.none();
    private boolean open;

    /** {@code source} gets the field's text and caret, and answers what could be typed there. */
    WaveScriptCompleter(HudTextField field, BiFunction<String, Integer, Result> source) {
        this.field = field;
        this.source = source;
        field.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Set.of());
        DocumentListener changes = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                WaveScriptCompleter.this.refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                WaveScriptCompleter.this.refresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                WaveScriptCompleter.this.refresh();
            }
        };
        field.getDocument().addDocumentListener(changes);
        CaretListener moves = evt -> this.refresh();
        field.addCaretListener(moves);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                WaveScriptCompleter.this.refresh();
            }

            @Override
            public void focusLost(FocusEvent e) {
                WaveScriptCompleter.this.close();
                WaveScriptCompleter.this.field.setGhostText("");
            }
        });
        this.wrap(KeyEvent.VK_DOWN, () -> this.step(1));
        this.wrap(KeyEvent.VK_UP, () -> this.open && this.step(-1));
        this.wrap(KeyEvent.VK_TAB, this::acceptBest);
        this.wrap(KeyEvent.VK_RIGHT, this::acceptGhost);
        this.wrap(KeyEvent.VK_ENTER, this::acceptSelected);
        this.wrap(KeyEvent.VK_ESCAPE, this::close);
    }

    /** Puts a handler before whatever the key did, which runs when the handler leaves the key alone. */
    private void wrap(int keyCode, BooleanSupplier handler) {
        KeyStroke stroke = KeyStroke.getKeyStroke(keyCode, 0);
        Object name = this.field.getInputMap().get(stroke);
        Action previous = name == null ? null : this.field.getActionMap().get(name);
        String own = "suggest-" + keyCode;
        this.field.getInputMap().put(stroke, own);
        this.field.getActionMap().put(own, new AbstractAction() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent evt) {
                if (!handler.getAsBoolean() && previous != null) {
                    previous.actionPerformed(evt);
                }
            }
        });
    }

    private void refresh() {
        String text = this.field.getText();
        int caret = Math.min(this.field.getCaretPosition(), text.length());
        this.result = this.source.apply(text, caret);
        String typed = text.substring(this.result.start(), caret);
        this.field.setGhostText(this.field.isFocusOwner()
                ? this.completion().map(c -> c.text().substring(typed.length())).orElse("")
                : "");
        if (this.open) {
            if (this.result.candidates().isEmpty()) {
                this.close();
            } else {
                this.list.show(this.result.candidates());
                this.place();
            }
        }
    }

    private boolean step(int delta) {
        if (this.result.candidates().isEmpty()) {
            return false;
        }
        if (this.open) {
            this.list.move(delta);
        } else {
            this.open = true;
            this.list.show(this.result.candidates());
        }
        this.place();
        return true;
    }

    /** The ghost: the best match, when the caret ends a part-typed token that it would lengthen. */
    private Optional<Candidate> completion() {
        String text = this.field.getText();
        int caret = this.field.getCaretPosition();
        if (caret != text.length() || this.field.getSelectionStart() != this.field.getSelectionEnd()) {
            return Optional.empty();
        }
        String typed = text.substring(this.result.start());
        return this.result.best().filter(best -> !typed.isEmpty() && best.text().length() > typed.length());
    }

    private boolean acceptBest() {
        if (this.open) {
            return this.acceptSelected();
        }
        Optional<Candidate> best = this.result.best();
        if (this.typedBeforeCaret().isEmpty() || best.isEmpty()) {
            this.field.transferFocus();
        } else {
            this.take(best.get());
        }
        return true;
    }

    private boolean acceptGhost() {
        Optional<Candidate> ghost = this.completion();
        ghost.ifPresent(this::take);
        return ghost.isPresent();
    }

    private String typedBeforeCaret() {
        return this.field.getText().substring(this.result.start(), this.field.getCaretPosition());
    }

    private boolean acceptSelected() {
        if (!this.open) {
            return false;
        }
        Candidate chosen = this.list.selected();
        this.close();
        this.take(chosen);
        return true;
    }

    /** Replaces the token under the caret, and leaves a space after a whole token at the end of the script. */
    private void take(Candidate candidate) {
        boolean atEnd = this.result.end() == this.field.getText().length();
        this.field.select(this.result.start(), this.result.end());
        this.field.replaceSelection(candidate.text() + (atEnd && candidate.complete() ? " " : ""));
    }

    private boolean close() {
        boolean wasOpen = this.open;
        this.open = false;
        if (this.list.getParent() != null) {
            JComponent parent = (JComponent) this.list.getParent();
            parent.remove(this.list);
            parent.repaint();
        }
        return wasOpen;
    }

    /** Hangs the list under the field, or above it where the window has no room below. */
    private void place() {
        JLayeredPane layers = SwingUtilities.getRootPane(this.field).getLayeredPane();
        Point below = SwingUtilities.convertPoint(this.field, 0, this.field.getHeight(), layers);
        int width = this.list.preferredWidth();
        int height = this.list.getPreferredSize().height;
        int x = Math.max(0, Math.min(below.x, layers.getWidth() - width));
        int y = below.y + height > layers.getHeight() ? below.y - this.field.getHeight() - height : below.y;
        this.list.setBounds(x, y, width, height);
        if (this.list.getParent() == null) {
            layers.add(this.list, JLayeredPane.POPUP_LAYER);
        }
        layers.repaint();
    }

    private static final class SuggestionList extends JComponent {

        @Serial
        private static final long serialVersionUID = 1L;

        private static final int MAX_ROWS = 8;
        private static final int ROW_HEIGHT = 16;
        private static final int PADDING = 6;
        private static final Color SELECTED_FILL = new Color(46, 66, 46);
        private static final Color HINT = new Color(150, 170, 150);

        private List<Candidate> rows = List.of();
        private int selected;
        private int top;

        void show(List<Candidate> candidates) {
            this.rows = candidates;
            this.selected = 0;
            this.top = 0;
            this.fit();
        }

        void move(int delta) {
            this.selected = Math.floorMod(this.selected + delta, this.rows.size());
            if (this.selected < this.top) {
                this.top = this.selected;
            } else if (this.selected >= this.top + MAX_ROWS) {
                this.top = this.selected - MAX_ROWS + 1;
            }
            repaint();
        }

        Candidate selected() {
            return this.rows.get(this.selected);
        }

        int preferredWidth() {
            FontMetrics metrics = getFontMetrics(Hud.LABEL_FONT);
            int widest = 0;
            for (Candidate row : this.rows) {
                widest = Math.max(widest, metrics.stringWidth(row.text()) + metrics.stringWidth(row.hint()));
            }
            return widest + 5 * PADDING;
        }

        private void fit() {
            setSize(getWidth(), Math.min(this.rows.size(), MAX_ROWS) * ROW_HEIGHT + 2);
            setPreferredSize(getSize());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Hud.BACKGROUND);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Hud.BORDER_IDLE);
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            g2.setFont(Hud.LABEL_FONT);
            FontMetrics metrics = g2.getFontMetrics();
            int shown = Math.min(this.rows.size() - this.top, MAX_ROWS);
            for (int i = 0; i < shown; i++) {
                Candidate row = this.rows.get(this.top + i);
                int y = 1 + i * ROW_HEIGHT;
                if (this.top + i == this.selected) {
                    g2.setColor(SELECTED_FILL);
                    g2.fillRect(1, y, getWidth() - 2, ROW_HEIGHT);
                }
                int baseline = y + (ROW_HEIGHT + metrics.getAscent() - metrics.getDescent()) / 2;
                g2.setColor(Hud.FOREGROUND);
                g2.drawString(row.text(), PADDING, baseline);
                g2.setColor(HINT);
                g2.drawString(row.hint(), getWidth() - PADDING - metrics.stringWidth(row.hint()), baseline);
            }
            g2.dispose();
        }
    }
}
