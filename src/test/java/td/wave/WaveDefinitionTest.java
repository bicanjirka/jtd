package td.wave;

import org.junit.jupiter.api.Test;
import td.enemy.Rank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaveDefinitionTest {

    @Test
    void theFourArgumentConstructorDefaultsToNormalSpeed() {
        WaveDefinition wave = new WaveDefinition("walker", Rank.GRUNT);

        assertThat(wave.speedMultiplier()).isEqualTo(1f);
    }

    @Test
    void withSpeedMultiplierReturnsACopyLeavingTheOriginalUnchanged() {
        WaveDefinition wave = new WaveDefinition("walker", Rank.GRUNT);

        WaveDefinition faster = wave.withSpeedMultiplier(1.5f);

        assertThat(wave.speedMultiplier()).isEqualTo(1f);
        assertThat(faster.speedMultiplier()).isEqualTo(1.5f);
        assertThat(faster.enemies()).isEqualTo("walker");
        assertThat(faster.rank()).isEqualTo(Rank.GRUNT);
    }

    @Test
    void aNonPositiveSpeedMultiplierIsRejected() {
        assertThatThrownBy(() -> new WaveDefinition("walker", Rank.GRUNT).withSpeedMultiplier(0f))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WaveDefinition("walker", Rank.GRUNT).withSpeedMultiplier(-1f))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aNegativeClearBonusIsRejected() {
        assertThatThrownBy(() -> new WaveDefinition("walker", Rank.GRUNT).withClearBonus(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
