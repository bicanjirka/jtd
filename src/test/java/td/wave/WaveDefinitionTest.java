package td.wave;

import org.junit.jupiter.api.Test;
import td.enemy.Rank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaveDefinitionTest {

    @Test
    void theFourArgumentConstructorDefaultsToNormalSpeed() {
        WaveDefinition wave = new WaveDefinition("c", 100, 3, Rank.GRUNT);

        assertThat(wave.speedMultiplier()).isEqualTo(1f);
    }

    @Test
    void withSpeedMultiplierReturnsACopyLeavingTheOriginalUnchanged() {
        WaveDefinition wave = new WaveDefinition("c", 100, 3, Rank.GRUNT);

        WaveDefinition faster = wave.withSpeedMultiplier(1.5f);

        assertThat(wave.speedMultiplier()).isEqualTo(1f);
        assertThat(faster.speedMultiplier()).isEqualTo(1.5f);
        assertThat(faster.enemies()).isEqualTo("c");
        assertThat(faster.hp()).isEqualTo(100);
        assertThat(faster.price()).isEqualTo(3);
        assertThat(faster.rank()).isEqualTo(Rank.GRUNT);
    }

    @Test
    void aNonPositiveSpeedMultiplierIsRejected() {
        assertThatThrownBy(() -> new WaveDefinition("c", 100, 3, Rank.GRUNT, 0f))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WaveDefinition("c", 100, 3, Rank.GRUNT, -1f))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
