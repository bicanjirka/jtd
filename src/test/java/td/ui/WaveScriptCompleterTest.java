package td.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import td.wave.WaveScriptSuggestions.Candidate;
import td.wave.WaveScriptSuggestions.Result;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EventDispatchThreadExtension.class)
class WaveScriptCompleterTest {

    private final HudTextField field = new HudTextField("", 14);
    private final JRootPane root = new JRootPane();
    private final AtomicInteger entered = new AtomicInteger();
    private final AtomicInteger escaped = new AtomicInteger();
    private final AtomicInteger movedRight = new AtomicInteger();

    WaveScriptCompleterTest() {
        this.bind(KeyEvent.VK_ENTER, this.entered);
        this.bind(KeyEvent.VK_ESCAPE, this.escaped);
        this.bind(KeyEvent.VK_RIGHT, this.movedRight);
        this.root.getContentPane().add(this.field);
        new WaveScriptCompleter(this.field, WaveScriptCompleterTest::suggest);
    }

    /** Counts presses of {@code keyCode}, standing in for what the panel binds to it. */
    private void bind(int keyCode, AtomicInteger presses) {
        this.field.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(keyCode, 0), "count-" + keyCode);
        this.field.getActionMap().put("count-" + keyCode, new AbstractAction() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent evt) {
                presses.incrementAndGet();
            }
        });
    }

    /** Suggests "alpha" and "alder" for any token that begins like them, and the stem "w" for one that begins "w". */
    private static Result suggest(String text, int caret) {
        int start = text.lastIndexOf(' ', caret - 1) + 1;
        String prefix = text.substring(start, Math.min(caret, text.length()));
        List<Candidate> all = List.of(Candidate.token("alpha", ""), Candidate.token("alder", ""), Candidate.stem("w", ""));
        return new Result(start, text.length(), all.stream().filter(c -> c.text().startsWith(prefix)).toList());
    }

    private void press(int keyCode) {
        Object name = this.field.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(keyCode, 0));
        this.field.getActionMap().get(name).actionPerformed(new ActionEvent(this.field, ActionEvent.ACTION_PERFORMED, ""));
    }

    @Test
    void tabTakesTheBestMatchAndLeavesASpaceAfterIt() {
        this.field.setText("2 al");

        this.press(KeyEvent.VK_TAB);

        assertThat(this.field.getText()).isEqualTo("2 alpha ");
    }

    @Test
    void tabWithNothingTypedInsertsNothing() {
        this.field.setText("2 ");

        this.press(KeyEvent.VK_TAB);

        assertThat(this.field.getText()).isEqualTo("2 ");
    }

    @Test
    void takingAStemLeavesTheCaretOnTheTokenToFinishIt() {
        this.field.setText("w");

        this.press(KeyEvent.VK_TAB);

        assertThat(this.field.getText()).isEqualTo("w");
    }

    @Test
    void downThenEnterTakesTheNextMatchInsteadOfApplyingTheField() {
        this.field.setText("al");

        this.press(KeyEvent.VK_DOWN);
        this.press(KeyEvent.VK_DOWN);
        this.press(KeyEvent.VK_ENTER);

        assertThat(this.field.getText()).isEqualTo("alder ");
        assertThat(this.entered).hasValue(0);
    }

    @Test
    void clickingARowTakesItAndLeavesTheFieldUnapplied() {
        this.field.setText("al");
        this.press(KeyEvent.VK_DOWN);
        Component list = this.root.getLayeredPane().getComponentsInLayer(JLayeredPane.POPUP_LAYER)[0];

        list.dispatchEvent(new MouseEvent(list, MouseEvent.MOUSE_CLICKED, 0, 0, 5, list.getHeight() - 2, 1, false));

        assertThat(this.field.getText()).isEqualTo("alder ");
        assertThat(this.entered).hasValue(0);
    }

    @Test
    void enterWithTheListClosedApplies() {
        this.field.setText("al");

        this.press(KeyEvent.VK_ENTER);

        assertThat(this.entered).hasValue(1);
        assertThat(this.field.getText()).isEqualTo("al");
    }

    @Test
    void escapeClosesTheListBeforeItLeavesTheField() {
        this.field.setText("al");
        this.press(KeyEvent.VK_DOWN);

        this.press(KeyEvent.VK_ESCAPE);
        assertThat(this.escaped).hasValue(0);
        this.press(KeyEvent.VK_ESCAPE);

        assertThat(this.escaped).hasValue(1);
    }

    @Test
    void rightTakesTheCompletionAtTheEndOfTheText() {
        this.field.setText("al");

        this.press(KeyEvent.VK_RIGHT);

        assertThat(this.field.getText()).isEqualTo("alpha ");
        assertThat(this.movedRight).hasValue(0);
    }

    @Test
    void rightInsideTheTextDoesWhatRightDidBefore() {
        this.field.setText("al");
        this.field.setCaretPosition(1);

        this.press(KeyEvent.VK_RIGHT);

        assertThat(this.field.getText()).isEqualTo("al");
        assertThat(this.movedRight).hasValue(1);
    }
}
