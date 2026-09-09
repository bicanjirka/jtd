package td.wave;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyFactory;

import static org.assertj.core.api.Assertions.assertThat;

/** WaveScript.parse is the wave mini-language's parser (see CLAUDE.md) - no GameWorld needed. */
class WaveScriptTest {

    @Test
    void plainTokensCountAsOneEnemyEach() {
        WaveContent content = WaveScript.parse("c e c");

        // "e" (Empty) is a filler mob and is excluded from enemyCount()
        assertThat(content.enemyCount()).isEqualTo(2);
        assertThat(content.spawnSequence()).hasSize(3);
    }

    @Test
    void numericPrefixMultipliesTheFollowingToken() {
        WaveContent content = WaveScript.parse("2 c");

        assertThat(content.enemyCount()).isEqualTo(2);
        assertThat(content.enemyCount(EnemyFactory.Enemy.Circle)).isEqualTo(2);
    }

    @Test
    void aCountAppliesOnlyToTheTokenImmediatelyFollowingItAndThenResets() {
        WaveContent content = WaveScript.parse("2 c s");

        assertThat(content.enemyCount(EnemyFactory.Enemy.Circle)).isEqualTo(2);
        assertThat(content.enemyCount(EnemyFactory.Enemy.Square)).isEqualTo(1);
    }

    @Test
    void enemySetAndPerEnemyCountReflectTheParsedTokens() {
        WaveContent content = WaveScript.parse("c e c");

        assertThat(content.enemySet()).containsExactlyInAnyOrder(EnemyFactory.Enemy.Circle, EnemyFactory.Enemy.Empty);
        assertThat(content.enemyCount(EnemyFactory.Enemy.Circle)).isEqualTo(2);
        assertThat(content.enemyCount(EnemyFactory.Enemy.Empty)).isEqualTo(1);
        assertThat(content.enemyCount(EnemyFactory.Enemy.Ghost)).isZero();
    }

    @Test
    void unrecognizedTokenIsTreatedAsMultiplierOneAndDoesNotThrow() {
        WaveContent content = WaveScript.parse("c ? c");

        assertThat(content.enemyCount()).isEqualTo(2);
    }

    @Test
    void emptyStringParsesToNoSpawns() {
        WaveContent content = WaveScript.parse("");

        assertThat(content.spawnSequence()).isEmpty();
    }
}
