package td.ui;

import td.util.ThreadConfined;
import td.wave.WaveScriptSuggestions.Candidate;
import td.wave.WaveScriptSuggestions.Result;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

/**
 * Suggests the next token of a field. The best match shows as ghost text that Tab or Right accepts; Down or Up opens
 * the list of every match, all of it in view, and Enter or a click takes a row. While the list is open the arrows move
 * in it, Left and Right between its columns. Esc closes the list before it leaves the field, Enter with the list
 * closed applies the field as ever, and Tab with nothing typed moves focus on. A row of a term or an enemy shows its
 * glyph, and hovering it opens its card.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class FieldCompleter {

    private final HudTextField field;
    private final BiFunction<String, Integer, Result> source;
    private final CandidateLooks looks;
    private final SuggestionList list;

    private Result result = Result.none();
    private boolean open;
    /** The open list's rows and their looks, set together. */
    private List<Candidate> shown = List.of();
    private List<Optional<RowLook>> shownLooks = List.of();
    private Runnable closeTooltips = () -> {
    };

    /** {@code source} gets the field's text and caret, and answers what could be typed there. */
    FieldCompleter(HudTextField field, BiFunction<String, Integer, Result> source, CandidateLooks looks) {
        this.field = field;
        this.source = source;
        this.looks = looks;
        this.list = new SuggestionList(index -> this.shownLooks.get(index), this::acceptSelected);
        field.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Set.of());
        DocumentListener changes = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                FieldCompleter.this.refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                FieldCompleter.this.refresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                FieldCompleter.this.refresh();
            }
        };
        field.getDocument().addDocumentListener(changes);
        CaretListener moves = evt -> this.refresh();
        field.addCaretListener(moves);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                FieldCompleter.this.refresh();
            }

            @Override
            public void focusLost(FocusEvent e) {
                FieldCompleter.this.close();
                FieldCompleter.this.field.setGhostText("");
            }
        });
        this.wrap(KeyEvent.VK_DOWN, () -> this.step(1));
        this.wrap(KeyEvent.VK_UP, () -> this.step(-1));
        this.wrap(KeyEvent.VK_TAB, this::acceptBest);
        this.wrap(KeyEvent.VK_RIGHT, () -> this.open ? this.across(1) : this.acceptGhost());
        this.wrap(KeyEvent.VK_LEFT, () -> this.open && this.across(-1));
        this.wrap(KeyEvent.VK_ENTER, this::acceptSelected);
        this.wrap(KeyEvent.VK_ESCAPE, this::close);
    }

    /** Hovering a row of the open list opens its card on {@code tooltips}; closing the list closes it. */
    void setTooltips(TooltipLayer tooltips) {
        tooltips.watchSpots(this.list, this::spotAt);
        this.closeTooltips = tooltips::closeAll;
    }

    private Optional<TooltipSpot> spotAt(Point point) {
        OptionalInt row = this.list.rowAt(point);
        if (row.isEmpty()) {
            return Optional.empty();
        }
        return this.looks.tooltip(this.shown.get(row.getAsInt()))
                .map(tooltip -> new TooltipSpot(tooltip, this.list.rowBounds(row.getAsInt())));
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
            } else if (!this.result.candidates().equals(this.shown)) {
                this.showAll();
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
            this.showAll();
        }
        return true;
    }

    private boolean across(int delta) {
        this.list.moveAcross(delta);
        return true;
    }

    /** Hangs every match, the card of a row it replaces closed. */
    private void showAll() {
        this.closeTooltips.run();
        this.shown = this.result.candidates();
        this.shownLooks = this.shown.stream().map(this.looks::look).toList();
        this.list.show(this.shown);
        this.list.hangInColumnsBy(this.field);
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
        if (this.list.isHung()) {
            this.closeTooltips.run();
        }
        this.list.takeDown();
        this.shown = List.of();
        return wasOpen;
    }
}
