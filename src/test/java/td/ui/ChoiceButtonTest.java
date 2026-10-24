package td.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EventDispatchThreadExtension.class)
class ChoiceButtonTest {

    private static final String ALPHA = "Alpha";
    private static final String BETA = "Beta";
    private static final String BRAVO = "Bravo";
    private static final int BUTTON_WIDTH = 50;
    private static final int BUTTON_HEIGHT = 20;
    /** A list row's height, so a window can be made exactly two rows short. */
    private static final int ROW_HEIGHT = 16;

    private final JRootPane root = new JRootPane();
    private final AtomicInteger closed = new AtomicInteger();
    private final ChoiceButton<String> choice = new ChoiceButton<>(choices(BRAVO, ALPHA, BETA), ALPHA,
            this.closed::incrementAndGet);

    ChoiceButtonTest() {
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

    @Test
    void aDigitJumpsToATitleWithAWordItStarts() {
        this.choice.offer(choices("Path 1", "Path 2"), "Path 1");
        this.choice.toggle();

        this.type('2');
        this.press(KeyEvent.VK_ENTER);

        assertThat(this.choice.chosen()).isEqualTo("Path 2");
    }

    @Test
    void offeringANewSetClosesTheListAndListsOnlyTheNewSet() {
        this.choice.toggle();

        this.choice.offer(choices("Gamma", "Delta"), "Gamma");
        assertThat(this.openList()).isNull();
        this.choice.toggle();
        this.press(KeyEvent.VK_END);
        this.press(KeyEvent.VK_ENTER);

        assertThat(this.choice.chosen()).isEqualTo("Gamma");
    }

    @Test
    void theKeysPassOverARowThatCannotBeTaken() {
        this.choice.offer(choicesWithoutBeta(), ALPHA);
        this.choice.toggle();

        this.press(KeyEvent.VK_DOWN);
        this.press(KeyEvent.VK_ENTER);

        assertThat(this.choice.chosen()).isEqualTo(BRAVO);
    }

    @Test
    void aLetterPassesOverARowThatCannotBeTaken() {
        this.choice.offer(choicesWithoutBeta(), ALPHA);
        this.choice.toggle();

        this.type('b');
        this.press(KeyEvent.VK_ENTER);

        assertThat(this.choice.chosen()).isEqualTo(BRAVO);
    }

    @Test
    void clickingARowThatCannotBeTakenTakesNothingAndLeavesTheListOpen() {
        this.choice.offer(choicesWithoutBeta(), ALPHA);
        this.choice.toggle();
        Component list = this.openList();

        list.dispatchEvent(new MouseEvent(list, MouseEvent.MOUSE_CLICKED, 0, 0, 5, 1 + ROW_HEIGHT + ROW_HEIGHT / 2, 1, false));

        assertThat(this.choice.chosen()).isEqualTo(ALPHA);
        assertThat(this.openList()).isNotNull();
    }

    private static Choices<String> choices(String... titles) {
        return new Choices<>(List.of(titles), Comparator.naturalOrder(), title -> title, title -> "", ChoiceButtonTest::look);
    }

    /** Alpha, Beta and Bravo in that order, Beta greyed out. */
    private static Choices<String> choicesWithoutBeta() {
        return new Choices<>(List.of(ALPHA, BETA, BRAVO), Comparator.naturalOrder(), title -> title, title -> "",
                ChoiceButtonTest::look, title -> title.equals(BETA) ? Optional.of("not here") : Optional.empty());
    }

    private static RowLook look(String title) {
        return new RowLook(new PaintedIcon(8, 8, g2 -> {
        }), Color.WHITE, Color.GRAY);
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
