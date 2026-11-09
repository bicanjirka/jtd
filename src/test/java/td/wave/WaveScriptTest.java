package td.wave;

import org.junit.jupiter.api.Test;
import td.enemy.BodyArchetype;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.Rank;
import td.enemy.SpawnParameters;
import td.fixtures.EnemyFixtures;
import td.util.GameStartupException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaveScriptTest {

    private final EnemyCatalog catalog = catalogWithAWarlord();

    /** The built-in enemies and a unique one of the test's own. */
    private static EnemyCatalog catalogWithAWarlord() {
        EnemyCatalog catalog = EnemyCatalog.builtIn();
        catalog.registerUnique(EnemyFixtures.simpleDefinition("warlord"));
        return catalog;
    }
    private final EnemyDefinition simple = this.catalog.get("walker");
    private final EnemyDefinition armored = this.catalog.get("knight");
    private final EnemyDefinition ghost = this.catalog.get("ghost");

    @Test
    void aLeadingSpacingTokenSetsTheWavesSpacingAndSpawnsNothing() {
        WaveContent content = WaveScript.parse("w30: 2 walker", Rank.GRUNT, this.catalog);

        assertThat(content.delayTicksPerSlot()).isEqualTo(30f);
        assertThat(content.enemyCount()).isEqualTo(2);
    }

    @Test
    void aWaveWithNoSpacingTokenUsesTheDefaultSpacing() {
        WaveContent content = WaveScript.parse("2 walker", Rank.GRUNT, this.catalog);

        assertThat(content.delayTicksPerSlot()).isEqualTo(SpawnParameters.DEFAULT_DELAY_TICKS_PER_SLOT);
    }

    @Test
    void aSpacingTokenOutsideAHeadFailsAtThatToken() {
        assertThatThrownBy(() -> WaveScript.parse("w30 walker", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class)
                .hasMessageContaining("»w30«")
                .hasMessageContaining("head");
    }

    @Test
    void aHeadsRankIsTheRankOfEverySlotThatNamesNone() {
        WaveContent content = WaveScript.parse("soldier: walker knight", Rank.GRUNT, this.catalog);

        assertThat(content.enemySet()).containsExactly(this.catalog.get("walker", Rank.SOLDIER),
                this.catalog.get("knight", Rank.SOLDIER));
    }

    @Test
    void aSlotsOwnRankBeatsTheHeads() {
        WaveContent content = WaveScript.parse("soldier: elite walker knight", Rank.GRUNT, this.catalog);

        assertThat(content.enemySet()).containsExactly(this.catalog.get("walker", Rank.ELITE),
                this.catalog.get("knight", Rank.SOLDIER));
    }

    @Test
    void aLevelsWaveWithNoRankInItsHeadIsGrunt() {
        WaveContent content = WaveScript.parse("w30: walker", this.catalog);

        assertThat(content.enemySet()).containsExactly(this.catalog.get("walker", Rank.GRUNT));
    }

    @Test
    void aHeadsEndMayStandApartFromItsLastWord() {
        WaveContent apart = WaveScript.parse("w30 soldier : walker", Rank.GRUNT, this.catalog);

        assertThat(apart.delayTicksPerSlot()).isEqualTo(30f);
        assertThat(apart.enemySet()).containsExactly(this.catalog.get("walker", Rank.SOLDIER));
    }

    @Test
    void aSecondHeadEndFails() {
        assertThatThrownBy(() -> WaveScript.parse("soldier: walker: knight", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class)
                .hasMessageContaining("one head");
    }

    @Test
    void aSlotBeforeTheHeadEndFailsAtItsFirstToken() {
        assertThatThrownBy(() -> WaveScript.parse("soldier 2 walker: knight", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class)
                .hasMessageContaining("»2«")
                .hasMessageContaining("A head holds only");
    }

    @Test
    void aRepeatCountAboveTheMaximumIsRejectedBeforeAnySlotIsBuilt() {
        assertThatThrownBy(() -> WaveScript.parse("999999999 walker", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class)
                .hasMessageContaining(String.valueOf(WaveScript.MAX_REPEAT));
    }

    @Test
    void aSpacingOfZeroIsRejected() {
        assertThatThrownBy(() -> WaveScript.parse("w0: walker", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void plainTokensCountAsOneEnemyEach() {
        WaveContent content = WaveScript.parse("walker e walker", Rank.GRUNT, this.catalog);

        // "e" is the reserved spacer token and is excluded from enemyCount()
        assertThat(content.enemyCount()).isEqualTo(2);
        assertThat(content.spawnSequence()).hasSize(3);
    }

    @Test
    void numericPrefixMultipliesTheFollowingToken() {
        WaveContent content = WaveScript.parse("2 walker", Rank.GRUNT, this.catalog);

        assertThat(content.enemyCount()).isEqualTo(2);
        assertThat(content.enemyCount(this.simple)).isEqualTo(2);
    }

    @Test
    void aNumericPrefixBeforeTheSpacerRepeatsItTheSameWayAsAnyOtherToken() {
        WaveContent fourSeparateSpacers = WaveScript.parse("walker e e e e walker", Rank.GRUNT, this.catalog);
        WaveContent oneCountedSpacer = WaveScript.parse("walker 4 e walker", Rank.GRUNT, this.catalog);

        assertThat(oneCountedSpacer.spawnSequence()).isEqualTo(fourSeparateSpacers.spawnSequence());
        assertThat(oneCountedSpacer.spawnSequence()).hasSize(6);
        assertThat(oneCountedSpacer.enemyCount()).isEqualTo(2);
    }

    @Test
    void aCountAppliesOnlyToTheTokenImmediatelyFollowingItAndThenResets() {
        WaveContent content = WaveScript.parse("2 walker knight", Rank.GRUNT, this.catalog);

        assertThat(content.enemyCount(this.simple)).isEqualTo(2);
        assertThat(content.enemyCount(this.armored)).isEqualTo(1);
    }

    @Test
    void enemySetAndPerEnemyCountReflectTheParsedTokens() {
        WaveContent content = WaveScript.parse("walker e walker", Rank.GRUNT, this.catalog);

        // the spacer never appears in enemySet()
        assertThat(content.enemySet()).containsExactly(this.simple);
        assertThat(content.enemyCount(this.simple)).isEqualTo(2);
        assertThat(content.enemyCount(this.ghost)).isZero();
    }

    @Test
    void anUnrecognizedTokenFailsTheParseRatherThanSilentlyChangingTheWave() {
        assertThatThrownBy(() -> WaveScript.parse("walker ? walker", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class)
                .hasMessageContaining("'?'")
                .hasMessageContaining("enemy catalog");
    }

    @Test
    void repeatedSpacesBetweenTokensAreNotThemselvesTokens() {
        WaveContent content = WaveScript.parse("walker   walker", Rank.GRUNT, this.catalog);

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

        WaveContent content = WaveScript.parse("walker tankySquare", Rank.GRUNT, perLevelCatalog);

        assertThat(content.enemySet()).containsExactlyInAnyOrder(this.simple, tankySquare);
        assertThat(content.enemyCount(tankySquare)).isEqualTo(1);
    }

    @Test
    void aSpawnTypeKeywordWithNoCountShapesExactlyOneSlot() {
        WaveContent content = WaveScript.parse("plated warlord", Rank.GRUNT, this.catalog);

        assertThat(content.spawnSequence()).hasSize(1);
        EnemySlot slot = (EnemySlot) content.spawnSequence().getFirst();
        assertThat(slot.shape()).isEqualTo(SpawnShape.armored());
        assertThat(content.enemyCount()).isEqualTo(1);
    }

    @Test
    void aCountBeforeASpawnTypeKeywordRepeatsTheWholeShapedSlot() {
        WaveContent content = WaveScript.parse("3 plated warlord", Rank.GRUNT, this.catalog);

        assertThat(content.spawnSequence()).hasSize(3);
        assertThat(content.enemyCount()).isEqualTo(3);
    }

    @Test
    void aCountAfterASpawnTypeKeywordSetsThatSlotsMemberCount() {
        WaveContent content = WaveScript.parse("swarm 4 walker", Rank.GRUNT, this.catalog);

        assertThat(content.spawnSequence()).hasSize(1);
        EnemySlot slot = (EnemySlot) content.spawnSequence().getFirst();
        assertThat(slot.shape()).isEqualTo(SpawnShape.swarm(4));
        assertThat(content.enemyCount()).isEqualTo(4);
    }

    @Test
    void aLeadingCountAndAMemberCountComposeIndependently() {
        WaveContent content = WaveScript.parse("3 swarm 4 walker", Rank.GRUNT, this.catalog);

        assertThat(content.spawnSequence()).hasSize(3);
        assertThat(content.enemyCount()).isEqualTo(12);
    }

    @Test
    void swarmLineColumnAndDripRequireAMemberCount() {
        assertThatThrownBy(() -> WaveScript.parse("swarm walker", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
        assertThatThrownBy(() -> WaveScript.parse("line walker", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
        assertThatThrownBy(() -> WaveScript.parse("column walker", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
        assertThatThrownBy(() -> WaveScript.parse("drip walker", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void armoredAndFlankRejectAMemberCount() {
        // A count before the spawn-type keyword ("3 plated walker") legally repeats the whole shaped
        // slot - it's a count immediately after the keyword, before the enemy id, that these two
        // shapes reject, since their member count is fixed.
        assertThatThrownBy(() -> WaveScript.parse("plated 3 walker", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
        assertThatThrownBy(() -> WaveScript.parse("flank 3 walker", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void flankAlwaysProducesExactlyTwoMembersWithNoCount() {
        WaveContent content = WaveScript.parse("flank walker", Rank.GRUNT, this.catalog);

        assertThat(content.enemyCount()).isEqualTo(2);
    }

    @Test
    void twoSpawnTypeKeywordsInARowFailTheParse() {
        assertThatThrownBy(() -> WaveScript.parse("swarm plated warlord", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void aWaveEndingWithADanglingSpawnTypeKeywordFailsTheParse() {
        assertThatThrownBy(() -> WaveScript.parse("walker swarm 3", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void theSpacerCannotFollowASpawnTypeKeyword() {
        assertThatThrownBy(() -> WaveScript.parse("swarm e", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void aSlotWithNoRankTokenUsesTheWavesDefaultRank() {
        WaveContent content = WaveScript.parse("walker", Rank.VETERAN, this.catalog);

        assertThat(content.rankFor(this.catalog.get("walker", Rank.VETERAN))).isEqualTo(Rank.VETERAN);
    }

    @Test
    void aRankTokenBeforeAnEnemyIdOverridesTheWavesDefaultForThatSlotOnly() {
        WaveContent content = WaveScript.parse("elite walker knight", Rank.GRUNT, this.catalog);

        EnemyDefinition eliteSimple = this.catalog.get("walker", Rank.ELITE);
        EnemyDefinition gruntArmored = this.catalog.get("knight", Rank.GRUNT);
        assertThat(content.enemySet()).containsExactlyInAnyOrder(eliteSimple, gruntArmored);
        assertThat(content.rankFor(eliteSimple)).isEqualTo(Rank.ELITE);
        assertThat(content.rankFor(gruntArmored)).isEqualTo(Rank.GRUNT);
    }

    @Test
    void aRankTokenAskingForARankTheEnemyDoesNotDefineFallsBackSilentlyToItsHighestRank() {
        EnemyDefinition oneRank = EnemyDefinition.of("oneRank", "One Rank", 100, 5, 1.28f, BodyArchetype.SQUARE);
        this.catalog.register(oneRank);

        WaveContent content = WaveScript.parse("boss oneRank", Rank.GRUNT, this.catalog);

        assertThat(content.rankFor(oneRank)).isEqualTo(Rank.GRUNT);
    }

    @Test
    void aUniqueEnemyComesAtBossRankWhateverRankItsWaveDefaultsTo() {
        WaveContent content = WaveScript.parse("warlord walker", Rank.GRUNT, this.catalog);

        assertThat(content.rankFor(this.catalog.get("warlord"))).isEqualTo(Rank.BOSS);
        assertThat(content.highestRank()).isEqualTo(Rank.BOSS);
        assertThat(content.hasUnique()).isTrue();
    }

    @Test
    void aRankTokenOtherThanBossBeforeAUniqueEnemyFailsTheParse() {
        assertThatThrownBy(() -> WaveScript.parse("elite warlord", Rank.BOSS, this.catalog))
                .isInstanceOf(GameStartupException.class)
                .hasMessageContaining("unique");
        assertThat(WaveScript.parse("boss warlord", Rank.GRUNT, this.catalog).hasUnique()).isTrue();
    }

    @Test
    void aWaveOfLadderedEnemiesHasNoUniqueEnemyEvenAtBossRank() {
        assertThat(WaveScript.parse("boss walker", Rank.GRUNT, this.catalog).hasUnique()).isFalse();
    }

    @Test
    void aRankTokenBeforeASpawnTypeKeywordShapesAndRanksTheSameSlot() {
        WaveContent content = WaveScript.parse("elite swarm 4 walker", Rank.GRUNT, this.catalog);

        assertThat(content.spawnSequence()).hasSize(1);
        EnemySlot slot = (EnemySlot) content.spawnSequence().getFirst();
        assertThat(slot.rank()).isEqualTo(Rank.ELITE);
        assertThat(slot.shape()).isEqualTo(SpawnShape.swarm(4));
        assertThat(slot.definition()).isEqualTo(this.catalog.get("walker", Rank.ELITE));
    }

    @Test
    void aCountBeforeARankTokenRepeatsTheWholeRankedAndShapedSlot() {
        WaveContent content = WaveScript.parse("3 elite swarm 4 walker", Rank.GRUNT, this.catalog);

        assertThat(content.spawnSequence()).hasSize(3);
        assertThat(content.enemyCount()).isEqualTo(12);
        for (WaveSlot slot : content.spawnSequence()) {
            assertThat(((EnemySlot) slot).rank()).isEqualTo(Rank.ELITE);
        }
    }

    @Test
    void aCountAfterARankTokenWinsOverOneBeforeIt() {
        WaveContent content = WaveScript.parse("2 elite 3 walker", Rank.GRUNT, this.catalog);

        assertThat(content.spawnSequence()).hasSize(3);
    }

    @Test
    void ofConsecutiveCountsOnlyTheLastOneRepeats() {
        WaveContent content = WaveScript.parse("2 3 walker", Rank.GRUNT, this.catalog);

        assertThat(content.enemyCount(this.simple)).isEqualTo(3);
    }

    @Test
    void aCountLeftAtTheEndOfTheScriptRepeatsNothing() {
        WaveContent content = WaveScript.parse("walker 3", Rank.GRUNT, this.catalog);

        assertThat(content.spawnSequence()).hasSize(1);
    }

    @Test
    void aCountBeforeARankTokenWithNoShapeRepeatsTheWholeRankedSlot() {
        WaveContent content = WaveScript.parse("3 elite walker", Rank.GRUNT, this.catalog);

        assertThat(content.spawnSequence()).hasSize(3);
        assertThat(content.enemyCount()).isEqualTo(3);
    }

    @Test
    void twoRankTokensInARowFailTheParse() {
        assertThatThrownBy(() -> WaveScript.parse("elite boss walker", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void aRankTokenAfterASpawnTypeKeywordFailsTheParse() {
        assertThatThrownBy(() -> WaveScript.parse("swarm elite 4 walker", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void theSpacerCannotFollowARankToken() {
        assertThatThrownBy(() -> WaveScript.parse("elite e", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void aWaveEndingWithADanglingRankTokenFailsTheParse() {
        assertThatThrownBy(() -> WaveScript.parse("walker elite", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class);
    }

    @Test
    void aParseFailureMarksTheOffendingTokenInTheScript() {
        assertThatThrownBy(() -> WaveScript.parse("2 walker e nonsense walker", Rank.GRUNT, this.catalog))
                .isInstanceOf(GameStartupException.class)
                .hasMessageContaining("token 4 'nonsense'")
                .hasMessageContaining("\"2 walker e »nonsense« walker\"");
    }

    @Test
    void aWaveEndingEarlyPointsPastTheLastToken() {
        assertThatThrownBy(() -> WaveScript.parse("walker elite", Rank.GRUNT, this.catalog))
                .hasMessageContaining("\"walker elite »«\"");
    }
}
