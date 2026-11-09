package td.wave;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaveDefinitionTest {

    @Test
    void theScriptOnlyConstructorDefaultsToNormalSpeed() {
        WaveDefinition wave = new WaveDefinition("walker");

        assertThat(wave.speedMultiplier()).isEqualTo(1f);
    }

    @Test
    void withSpeedMultiplierReturnsACopyLeavingTheOriginalUnchanged() {
        WaveDefinition wave = new WaveDefinition("walker");

        WaveDefinition faster = wave.withSpeedMultiplier(1.5f);

        assertThat(wave.speedMultiplier()).isEqualTo(1f);
        assertThat(faster.speedMultiplier()).isEqualTo(1.5f);
        assertThat(faster.enemies()).isEqualTo("walker");
    }

    @Test
    void aNonPositiveSpeedMultiplierIsRejected() {
        assertThatThrownBy(() -> new WaveDefinition("walker").withSpeedMultiplier(0f))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WaveDefinition("walker").withSpeedMultiplier(-1f))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aNegativeClearBonusIsRejected() {
        assertThatThrownBy(() -> new WaveDefinition("walker").withClearBonus(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
