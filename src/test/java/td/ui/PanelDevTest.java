package td.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.KeyStroke;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EventDispatchThreadExtension.class)
class PanelDevTest {

    private final PanelDev panel = new PanelDev();

    @Test
    void aStrokesChipReadsShiftFirstThenItsKey() {
        KeyStroke clearAll = KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
        KeyStroke slower = KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, InputEvent.ALT_DOWN_MASK);

        assertThat(PanelDev.chipLabel(clearAll)).isEqualTo("⇧K");
        assertThat(PanelDev.chipLabel(slower)).isEqualTo(",");
    }

    @Test
    void holdingAltShowsEachControlsChipOverItAndLettingGoTakesThemAway() {
        HudButton killAll = button(this.panel, "Kill all").orElseThrow();

        this.panel.revealChips(true);
        String held = killAll.face().chip();
        this.panel.revealChips(false);

        assertThat(held).isEqualTo("K");
        assertThat(killAll.face().overlay()).isTrue();
        assertThat(killAll.face().hasChip()).isFalse();
    }

    private static Optional<HudButton> button(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof HudButton button && text.equals(button.getText())) {
                return Optional.of(button);
            }
            if (child instanceof Container container) {
                Optional<HudButton> found = button(container, text);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }
}
