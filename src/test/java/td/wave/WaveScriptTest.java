package td.wave;

import org.junit.jupiter.api.Test;
import td.enemy.BodyArchetype;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.util.GameStartupException;

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
    void aNumericPrefixBeforeTheSpacerRepeatsItTheSameWayAsAnyOtherToken() {
        WaveContent fourSeparateSpacers = WaveScript.parse("c e e e e c", this.catalog);
        WaveContent oneCountedSpacer = WaveScript.parse("c 4 e c", this.catalog);

        assertThat(oneCountedSpacer.spawnSequence()).isEqualTo(fourSeparateSpacers.spawnSequence());
        assertThat(oneCountedSpacer.spawnSequence()).hasSize(6);
        assertThat(oneCountedSpacer.enemyCount()).isEqualTo(2);
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
        EnemyDefinition tankySquare = EnemyDefinition.of("tankySquare", "Tanky Square", 100, 5, 1.28f,
                BodyArchetype.SQUARE);
        EnemyCatalog perLevelCatalog = EnemyCatalog.builtIn();
        perLevelCatalog.register(tankySquare);

        WaveContent content = WaveScript.parse("c tankySquare", perLevelCatalog);

        assertThat(content.enemySet()).containsExactlyInAnyOrder(this.simple, tankySquare);
        assertThat(content.enemyCount(tankySquare)).isEqualTo(1);
    }

    @Test
    void aSpawnTypeKeywordWithNoCountShapesExactlyOneSlot() {
        WaveContent content = WaveScript.parse("boss warden1", this.catalog);

        assertThat(content.spawnSequence()).hasSize(1);
        EnemySlot slot = (EnemySlot) content.spawnSequence().getFirst();
        assertThat(slot.shape()).isEqualTo(SpawnShape.boss());
        assertThat(content.enemyCount()).isEqualTo(1);
    }

    @Test
    void aCountBeforeASpawnTypeKeywordRepeatsTheWholeShapedSlot() {
        WaveContent content = WaveScript.parse("3 boss warden1", this.catalog);

        assertThat(content.spawnSequence()).hasSize(3);
        assertThat(content.enemyCount()).isEqualTo(3);
    }

    @Test
    void aCountAfterASpawnTypeKeywordSetsThatSlotsMemberCount() {
        WaveContent content = WaveScript.parse("swarm 4 c", this.catalog);

        assertThat(content.spawnSequence()).hasSize(1);
        EnemySlot slot = (EnemySlot) content.spawnSequence().getFirst();
        assertThat(slot.shape()).isEqualTo(SpawnShape.swarm(4));
        assertThat(content.enemyCount()).isEqualTo(4);
    }

    @Test
    void aLeadingCountAndAMemberCountComposeIndependently() {
        WaveContent content = WaveScript.parse("3 swarm 4 c", this.catalog);

        assertThat(content.spawnSequence()).hasSize(3);
        assertThat(content.enemyCount()).isEqualTo(12);
    }

    @Test
    void swarmLineColumnAndDripRequireAMemberCount() {
        assertThatThrownBy(() -> WaveScript.parse("swarm c", this.catalog))
                .isInstanceOf(GameStartupException.class);
        assertThatThrownBy(() -> WaveScript.parse("line c", this.catalog))
                .isInstanceOf(GameStartupException.class);
        assertThatThrownBy(() -> WaveScript.parse("column c", this.catalog))
                .isInstanceOf(GameStartupException.class);
        assertThatThrownBy(() -> WaveScript.parse("drip c", this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void bossEliteAndFlankRejectAMemberCount() {
        // A count before the spawn-type keyword ("3 boss c") legally repeats the whole shaped
        // slot - it's a count immediately after the keyword, before the enemy id, that these
        // three shapes reject, since their member count is fixed.
        assertThatThrownBy(() -> WaveScript.parse("boss 3 c", this.catalog))
                .isInstanceOf(GameStartupException.class);
        assertThatThrownBy(() -> WaveScript.parse("elite 3 c", this.catalog))
                .isInstanceOf(GameStartupException.class);
        assertThatThrownBy(() -> WaveScript.parse("flank 3 c", this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void flankAlwaysProducesExactlyTwoMembersWithNoCount() {
        WaveContent content = WaveScript.parse("flank c", this.catalog);

        assertThat(content.enemyCount()).isEqualTo(2);
    }

    @Test
    void twoSpawnTypeKeywordsInARowFailTheParse() {
        assertThatThrownBy(() -> WaveScript.parse("swarm boss warden1", this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void aWaveEndingWithADanglingSpawnTypeKeywordFailsTheParse() {
        assertThatThrownBy(() -> WaveScript.parse("c swarm 3", this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void theSpacerCannotFollowASpawnTypeKeyword() {
        assertThatThrownBy(() -> WaveScript.parse("swarm e", this.catalog))
                .isInstanceOf(GameStartupException.class);
    }
}
