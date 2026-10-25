package td.ui;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Optional;

/**
 * The game's keys besides a tower's ({@code TowerFactory.Type.placementKey}) and an upgrade's
 * ({@code UpgradeTree.keyOf}): what the frame runs on a key press, and the label the control it presses shows on its
 * chip, from one place so the two never drift apart.
 */
public enum GameKey {
    PAUSE(KeyEvent.VK_SPACE, "␣"),
    PLAY(KeyEvent.VK_D, "D"),
    FAST(KeyEvent.VK_F, "F"),
    SUPER_FAST(KeyEvent.VK_G, "G"),
    SELL(KeyEvent.VK_DELETE, "Del"),
    MENU(KeyEvent.VK_M, "M"),
    CONFIRM(KeyEvent.VK_ENTER, "↵"),
    CANCEL(KeyEvent.VK_ESCAPE, "Esc");

    private final int keyCode;
    private final String chip;

    GameKey(int keyCode, String chip) {
        this.keyCode = keyCode;
        this.chip = chip;
    }

    /** The key a key press with this code means, if any. */
    public static Optional<GameKey> of(int keyCode) {
        return Arrays.stream(values()).filter(key -> key.keyCode == keyCode).findFirst();
    }

    int keyCode() {
        return this.keyCode;
    }

    String chip() {
        return this.chip;
    }
}
