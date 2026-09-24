package td.wave;

import org.junit.jupiter.api.Test;
import td.enemy.BodyArchetype;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.Rank;
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
        WaveContent content = WaveScript.parse("c e c", Rank.GRUNT, this.catalog);

        // "e" is the reserved spacer token and is excluded from enemyCount()
        assertThat(content.enemyCount()).isEqualTo(2);
        assertThat(content.spawnSequence()).hasSize(3);
    }

    @Test
    void numericPrefixMultipliesTheFollowingToken() {
        WaveContent content = WaveScript.parse("2 c", Rank.GRUNT, this.catalog);

        assertThat(content.enemyCount()).isEqualTo(2);
        assertThat(content.enemyCount(this.simple)).isEqualTo(2);
    }

    @Test
    void aNumericPrefixBeforeTheSpacerRepeatsItTheSameWayAsAnyOtherToken() {
        WaveContent fourSeparateSpacers = WaveScript.parse("c e e e e c", Rank.GRUNT, this.catalog);
        WaveContent oneCountedSpacer = WaveScript.parse("c 4 e c", Rank.GRUNT, this.catalog);

        assertThat(oneCountedSpacer.spawnSequence()).isEqualTo(fourSeparateSpacers.spawnSequence());
        assertThat(oneCountedSpacer.spawnSequence()).hasSize(6);
        assertThat(oneCountedSpacer.enemyCount()).isEqualTo(2);
    }

    @Test
    void aCountAppliesOnlyToTheTokenImmediatelyFollowingItAndThenResets() {
        WaveContent content = WaveScript.parse("2 c s", Rank.GRUNT, this.catalog);

        assertThat(content.enemyCount(this.simple)).isEqualTo(2);
        assertThat(content.enemyCount(this.armored)).isEqualTo(1);
    }

    @Test
    void enemySetAndPerEnemyCountReflectTheParsedTokens() {
        WaveContent content = WaveScript.parse("c e c", Rank.GRUNT, this.catalog);

        // the spacer never appears in enemySet()
        assertThat(content.enemySet()).containsExactly(this.simple);
        assertThat(content.enemyCount(this.simple)).isEqualTo(2);
        assertThat(content.enemyCount(this.ghost)).isZero();
    }

    @Test
    void anUnrecognizedTokenFailsTheParseRatherThanSilentlyChangingTheWave() {
        assertThatThrownBy(() -> WaveScript.parse("c ? c", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class)
                .hasMessageContaining("'?'")
                .hasMessageContaining("enemy catalog");
    }

    @Test
    void repeatedSpacesBetweenTokensAreNotThemselvesTokens() {
        WaveContent content = WaveScript.parse("c   c", Rank.GRUNT, this.catalog);

        assertThat(content.enemyCount()).isEqualTo(2);
    }

    @Test
    void emptyStringParsesToNoSpawns() {
        WaveContent content = WaveScript.parse("", Rank.GRUNT, this.catalog);

        assertThat(content.spawnSequence()).isEmpty();
    }

    @Test
    void aPerLevelCustomIdResolvesTheSameWayABuiltInDoes() {
        EnemyDefinition tankySquare = EnemyDefinition.of("tankySquare", "Tanky Square", 100, 5, 1.28f,
                BodyArchetype.SQUARE);
        EnemyCatalog perLevelCatalog = EnemyCatalog.builtIn();
        perLevelCatalog.register(tankySquare);

        WaveContent content = WaveScript.parse("c tankySquare", Rank.GRUNT, perLevelCatalog);

        assertThat(content.enemySet()).containsExactlyInAnyOrder(this.simple, tankySquare);
        assertThat(content.enemyCount(tankySquare)).isEqualTo(1);
    }

    @Test
    void aSpawnTypeKeywordWithNoCountShapesExactlyOneSlot() {
        WaveContent content = WaveScript.parse("armored warden1", Rank.GRUNT, this.catalog);

        assertThat(content.spawnSequence()).hasSize(1);
        EnemySlot slot = (EnemySlot) content.spawnSequence().getFirst();
        assertThat(slot.shape()).isEqualTo(SpawnShape.armored());
        assertThat(content.enemyCount()).isEqualTo(1);
    }

    @Test
    void aCountBeforeASpawnTypeKeywordRepeatsTheWholeShapedSlot() {
        WaveContent content = WaveScript.parse("3 armored warden1", Rank.GRUNT, this.catalog);

        assertThat(content.spawnSequence()).hasSize(3);
        assertThat(content.enemyCount()).isEqualTo(3);
    }

    @Test
    void aCountAfterASpawnTypeKeywordSetsThatSlotsMemberCount() {
        WaveContent content = WaveScript.parse("swarm 4 c", Rank.GRUNT, this.catalog);

        assertThat(content.spawnSequence()).hasSize(1);
        EnemySlot slot = (EnemySlot) content.spawnSequence().getFirst();
        assertThat(slot.shape()).isEqualTo(SpawnShape.swarm(4));
        assertThat(content.enemyCount()).isEqualTo(4);
    }

    @Test
    void aLeadingCountAndAMemberCountComposeIndependently() {
        WaveContent content = WaveScript.parse("3 swarm 4 c", Rank.GRUNT, this.catalog);

        assertThat(content.spawnSequence()).hasSize(3);
        assertThat(content.enemyCount()).isEqualTo(12);
    }

    @Test
    void swarmLineColumnAndDripRequireAMemberCount() {
        assertThatThrownBy(() -> WaveScript.parse("swarm c", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
        assertThatThrownBy(() -> WaveScript.parse("line c", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
        assertThatThrownBy(() -> WaveScript.parse("column c", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
        assertThatThrownBy(() -> WaveScript.parse("drip c", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void armoredAndFlankRejectAMemberCount() {
        // A count before the spawn-type keyword ("3 armored c") legally repeats the whole shaped
        // slot - it's a count immediately after the keyword, before the enemy id, that these two
        // shapes reject, since their member count is fixed.
        assertThatThrownBy(() -> WaveScript.parse("armored 3 c", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
        assertThatThrownBy(() -> WaveScript.parse("flank 3 c", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void flankAlwaysProducesExactlyTwoMembersWithNoCount() {
        WaveContent content = WaveScript.parse("flank c", Rank.GRUNT, this.catalog);

        assertThat(content.enemyCount()).isEqualTo(2);
    }

    @Test
    void twoSpawnTypeKeywordsInARowFailTheParse() {
        assertThatThrownBy(() -> WaveScript.parse("swarm armored warden1", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void aWaveEndingWithADanglingSpawnTypeKeywordFailsTheParse() {
        assertThatThrownBy(() -> WaveScript.parse("c swarm 3", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void theSpacerCannotFollowASpawnTypeKeyword() {
        assertThatThrownBy(() -> WaveScript.parse("swarm e", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void aSlotWithNoRankTokenUsesTheWavesDefaultRank() {
        WaveContent content = WaveScript.parse("c", Rank.VETERAN, this.catalog);

        assertThat(content.rankFor(this.catalog.get("c", Rank.VETERAN))).isEqualTo(Rank.VETERAN);
    }

    @Test
    void aRankTokenBeforeAnEnemyIdOverridesTheWavesDefaultForThatSlotOnly() {
        WaveContent content = WaveScript.parse("elite c s", Rank.GRUNT, this.catalog);

        EnemyDefinition eliteSimple = this.catalog.get("c", Rank.ELITE);
        EnemyDefinition gruntArmored = this.catalog.get("s", Rank.GRUNT);
        assertThat(content.enemySet()).containsExactlyInAnyOrder(eliteSimple, gruntArmored);
        assertThat(content.rankFor(eliteSimple)).isEqualTo(Rank.ELITE);
        assertThat(content.rankFor(gruntArmored)).isEqualTo(Rank.GRUNT);
    }

    @Test
    void aRankTokenAskingForARankTheEnemyDoesNotDefineFallsBackSilentlyToItsHighestRank() {
        WaveContent content = WaveScript.parse("boss wardenEgg1", Rank.GRUNT, this.catalog);

        EnemyDefinition wardenEgg = this.catalog.get("wardenEgg1");
        assertThat(content.rankFor(wardenEgg)).isEqualTo(Rank.GRUNT);
    }

    @Test
    void aRankTokenBeforeASpawnTypeKeywordShapesAndRanksTheSameSlot() {
        WaveContent content = WaveScript.parse("elite swarm 4 c", Rank.GRUNT, this.catalog);

        assertThat(content.spawnSequence()).hasSize(1);
        EnemySlot slot = (EnemySlot) content.spawnSequence().getFirst();
        assertThat(slot.rank()).isEqualTo(Rank.ELITE);
        assertThat(slot.shape()).isEqualTo(SpawnShape.swarm(4));
        assertThat(slot.definition()).isEqualTo(this.catalog.get("c", Rank.ELITE));
    }

    @Test
    void aCountBeforeARankTokenRepeatsTheWholeRankedAndShapedSlot() {
        WaveContent content = WaveScript.parse("3 elite swarm 4 c", Rank.GRUNT, this.catalog);

        assertThat(content.spawnSequence()).hasSize(3);
        assertThat(content.enemyCount()).isEqualTo(12);
        for (WaveSlot slot : content.spawnSequence()) {
            assertThat(((EnemySlot) slot).rank()).isEqualTo(Rank.ELITE);
        }
    }

    @Test
    void aCountBeforeARankTokenWithNoShapeRepeatsTheWholeRankedSlot() {
        WaveContent content = WaveScript.parse("3 elite c", Rank.GRUNT, this.catalog);

        assertThat(content.spawnSequence()).hasSize(3);
        assertThat(content.enemyCount()).isEqualTo(3);
    }

    @Test
    void twoRankTokensInARowFailTheParse() {
        assertThatThrownBy(() -> WaveScript.parse("elite boss c", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void aRankTokenAfterASpawnTypeKeywordFailsTheParse() {
        assertThatThrownBy(() -> WaveScript.parse("swarm elite 4 c", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void theSpacerCannotFollowARankToken() {
        assertThatThrownBy(() -> WaveScript.parse("elite e", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void aWaveEndingWithADanglingRankTokenFailsTheParse() {
        assertThatThrownBy(() -> WaveScript.parse("c elite", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
    }
}
