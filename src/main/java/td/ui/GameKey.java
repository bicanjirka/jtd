package td.ui;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Optional;

/**
 * The game's keys besides a tower's ({@code TowerFactory.Type.placementKey}) and an upgrade's
 * ({@code UpgradeTree.keyOf}): what the frame runs on a key press, and the label the control it presses shows on its
 * chip, from one place so the two never drift apart. A key may answer to more than one code; its chip names the first.
 */
public enum GameKey {
    PAUSE("Space", KeyEvent.VK_SPACE, KeyEvent.VK_P),
    PLAY("D", KeyEvent.VK_D),
    FAST("F", KeyEvent.VK_F),
    SUPER_FAST("G", KeyEvent.VK_G),
    SELL("Del", KeyEvent.VK_DELETE),
    MENU("M", KeyEvent.VK_M),
    RESTART("Z", KeyEvent.VK_Z),
    CONFIRM("↵", KeyEvent.VK_ENTER),
    CANCEL("Esc", KeyEvent.VK_ESCAPE);

    private final String chip;
    private final int[] keyCodes;

    GameKey(String chip, int... keyCodes) {
        this.chip = chip;
        this.keyCodes = keyCodes.clone();
    }

    /** The key a key press with this code means, if any. */
    public static Optional<GameKey> of(int keyCode) {
        return Arrays.stream(values()).filter(key -> key.answersTo(keyCode)).findFirst();
    }

    /** Whether a key press with this code means this key. */
    boolean answersTo(int keyCode) {
        return Arrays.stream(this.keyCodes).anyMatch(code -> code == keyCode);
    }

    String chip() {
        return this.chip;
    }
}
