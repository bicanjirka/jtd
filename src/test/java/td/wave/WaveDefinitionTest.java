package td.wave;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaveDefinitionTest {

    @Test
    void aNegativeClearBonusIsRejected() {
        assertThatThrownBy(() -> new WaveDefinition("walker").withClearBonus(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
