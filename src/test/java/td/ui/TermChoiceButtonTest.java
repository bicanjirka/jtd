package td.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import td.fixtures.FakeTerm;
import td.term.TermText;
import td.ui.render.IconFace;
import td.ui.render.Palette;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EventDispatchThreadExtension.class)
class TermChoiceButtonTest {

    private static final FakeTerm ALPHA = new FakeTerm("t.alpha", "Alpha", TermText.empty());
    private static final FakeTerm BETA = new FakeTerm("t.beta", "Beta", TermText.empty());
    private static final FakeTerm BRAVO = new FakeTerm("t.bravo", "Bravo", TermText.empty());
    private static final int BUTTON_WIDTH = 50;
    private static final int BUTTON_HEIGHT = 20;
    /** A list row's height, so a window can be made exactly two rows short. */
    private static final int ROW_HEIGHT = 16;

    private final JRootPane root = new JRootPane();
    private final AtomicInteger closed = new AtomicInteger();
    private final TermChoiceButton<FakeTerm> choice = new TermChoiceButton<>(
            new TermPicker<>(List.of(BRAVO, ALPHA, BETA), Comparator.comparing(term -> ""), term -> "",
                    term -> IconFace.of(term.key(), "", Palette.ENEMY_CIRCLE, Palette.ENEMY_CIRCLE)),
            ALPHA, this.closed::incrementAndGet);

    TermChoiceButtonTest() {
        this.root.getContentPane().setLayout(null);
        this.root.getContentPane().add(this.choice.component());
        this.choice.component().setBounds(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT);
        this.windowHeight(400);
    }

    @Test
    void aWindowTooShortForOneColumnSharesTheRowsBetweenColumnsAndRightMovesAcross() {
        this.windowHeight(BUTTON_HEIGHT + 2 + 2 * ROW_HEIGHT);
        this.choice.toggle();

        this.press(KeyEvent.VK_RIGHT);
        this.press(KeyEvent.VK_ENTER);

        assertThat(this.choice.chosen()).isEqualTo(BRAVO);
    }

    private void windowHeight(int height) {
        this.root.setSize(400, height);
        this.root.doLayout();
    }

    @Test
    void clickingTheButtonOpensTheListOnTheChosenTermAndDownThenEnterTakesTheNext() {
        ((AbstractButton) this.choice.component()).doClick();

        this.press(KeyEvent.VK_DOWN);
        this.press(KeyEvent.VK_ENTER);

        assertThat(this.choice.chosen()).isEqualTo(BETA);
        assertThat(this.openList()).isNull();
        assertThat(this.closed).hasValue(1);
    }

    @Test
    void aLetterJumpsToTheNextTitleItStartsWrappingRound() {
        this.choice.toggle();

        this.type('b');
        this.type('b');
        this.type('b');
        this.press(KeyEvent.VK_SPACE);

        assertThat(this.choice.chosen()).isEqualTo(BETA);
    }

    @Test
    void escapeClosesTheListAndKeepsTheChoice() {
        this.choice.toggle();
        this.press(KeyEvent.VK_END);

        this.press(KeyEvent.VK_ESCAPE);

        assertThat(this.choice.chosen()).isEqualTo(ALPHA);
        assertThat(this.openList()).isNull();
        assertThat(this.closed).hasValue(1);
    }

    @Test
    void clickingARowTakesIt() {
        this.choice.toggle();
        Component list = this.openList();

        list.dispatchEvent(new MouseEvent(list, MouseEvent.MOUSE_CLICKED, 0, 0, 5, list.getHeight() - 2, 1, false));

        assertThat(this.choice.chosen()).isEqualTo(BRAVO);
    }

    @Test
    void clickingTheButtonAgainClosesTheList() {
        this.choice.toggle();

        ((AbstractButton) this.choice.component()).doClick();

        assertThat(this.openList()).isNull();
        assertThat(this.choice.chosen()).isEqualTo(ALPHA);
    }

    private Component openList() {
        Component[] popups = this.root.getLayeredPane().getComponentsInLayer(JLayeredPane.POPUP_LAYER);
        return popups.length == 0 ? null : popups[0];
    }

    private void press(int keyCode) {
        JComponent list = (JComponent) this.openList();
        Object name = list.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(keyCode, 0));
        list.getActionMap().get(name).actionPerformed(new ActionEvent(list, ActionEvent.ACTION_PERFORMED, ""));
    }

    /** Straight to the list's listeners: a dispatched key goes to the focus owner, which a test window has none of. */
    private void type(char key) {
        Component list = this.openList();
        KeyEvent typed = new KeyEvent(list, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED, key);
        for (KeyListener listener : list.getKeyListeners()) {
            listener.keyTyped(typed);
        }
    }
}
