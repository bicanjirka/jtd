package td.ui;

import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;

import static org.assertj.core.api.Assertions.assertThat;

class GameKeyTest {

    @Test
    void pAndSpaceBothPause() {
        assertThat(GameKey.of(KeyEvent.VK_P)).contains(GameKey.PAUSE);
        assertThat(GameKey.of(KeyEvent.VK_SPACE)).contains(GameKey.PAUSE);
    }
}
