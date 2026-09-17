package td.wave;

import org.junit.jupiter.api.Test;
import td.enemy.BodyArchetype;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.enemy.FixedMovement;
import td.util.GameStartupException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * WaveScript.parse is the wave mini-language's parser (see CLAUDE.md) - no GameWorld needed.
 */
class WaveScriptTest {

    private final EnemyCatalog catalog = EnemyCatalog.builtIn();
    private final EnemyDefinition simple = this.catalog.get("c");
    private final EnemyDefinition armored = this.catalog.get("s");
    private final EnemyDefinition ghost = this.catalog.get("g");

    @Test
    void plainTokensCountAsOneEnemyEach() {
        WaveContent content = WaveScript.parse("c e c", this.catalog);

        // "e" is the reserved spacer token and is excluded from enemyCount()
        assertThat(content.enemyCount()).isEqualTo(2);
        assertThat(content.spawnSequence()).hasSize(3);
    }

    @Test
    void numericPrefixMultipliesTheFollowingToken() {
        WaveContent content = WaveScript.parse("2 c", this.catalog);

        assertThat(content.enemyCount()).isEqualTo(2);
        assertThat(content.enemyCount(this.simple)).isEqualTo(2);
    }

    @Test
    void aCountAppliesOnlyToTheTokenImmediatelyFollowingItAndThenResets() {
        WaveContent content = WaveScript.parse("2 c s", this.catalog);

        assertThat(content.enemyCount(this.simple)).isEqualTo(2);
        assertThat(content.enemyCount(this.armored)).isEqualTo(1);
    }

    @Test
    void enemySetAndPerEnemyCountReflectTheParsedTokens() {
        WaveContent content = WaveScript.parse("c e c", this.catalog);

        // the spacer never appears in enemySet(), unlike the old EnemyFactory.Enemy-keyed model
        assertThat(content.enemySet()).containsExactly(this.simple);
        assertThat(content.enemyCount(this.simple)).isEqualTo(2);
        assertThat(content.enemyCount(this.ghost)).isZero();
    }

    @Test
    void anUnrecognizedTokenFailsTheParseRatherThanSilentlyChangingTheWave() {
        assertThatThrownBy(() -> WaveScript.parse("c ? c", this.catalog))
                .isInstanceOf(GameStartupException.class)
                .hasMessageContaining("'?'")
                .hasMessageContaining("enemy catalog");
    }

    @Test
    void repeatedSpacesBetweenTokensAreNotThemselvesTokens() {
        WaveContent content = WaveScript.parse("c   c", this.catalog);

        assertThat(content.enemyCount()).isEqualTo(2);
    }

    @Test
    void emptyStringParsesToNoSpawns() {
        WaveContent content = WaveScript.parse("", this.catalog);

        assertThat(content.spawnSequence()).isEmpty();
    }

    @Test
    void aPerLevelCustomIdResolvesTheSameWayABuiltInDoes() {
        EnemyDefinition tankySquare = new EnemyDefinition("tankySquare", "Tanky Square", "", 100, 5, 1.28f, 1f,
                EnemyMob.Type.NORMAL, BodyArchetype.SQUARE, new FixedMovement(), List.of(), List.of());
        EnemyCatalog perLevelCatalog = EnemyCatalog.builtIn();
        perLevelCatalog.register(tankySquare);

        WaveContent content = WaveScript.parse("c tankySquare", perLevelCatalog);

        assertThat(content.enemySet()).containsExactlyInAnyOrder(this.simple, tankySquare);
        assertThat(content.enemyCount(tankySquare)).isEqualTo(1);
    }
}
