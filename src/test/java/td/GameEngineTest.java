package td;

import org.junit.jupiter.api.Test;
import td.enemy.BodyArchetype;
import td.enemy.EnemyDefinition;
import td.enemy.Rank;
import td.fixtures.BoardFixtures;
import td.fixtures.LevelFixtures;
import td.level.LevelDefinition;
import td.projectile.CannonballProjectile;
import td.tower.MortarTower;
import td.tower.SniperTower;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.LoadedLevel;
import td.wave.WaveDefinition;
import td.wave.WaveProgress;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * End-to-end tests driven entirely through GameEngine's public API - the
 * same methods TowerDefence's mouse/keyboard listeners call - asserting on
 * resulting GameWorld state. No window, no AWT event, no real clock: doTick()
 * is called with explicit tick numbers instead of relying on the real game
 * loop's timing.
 */
class GameEngineTest {

    // A trivially weak custom enemy for tests that just want a guaranteed one-shot kill within
    // a small, deterministic tick budget, independent of whichever real health/bounty a built-in
    // enemy's own rank ladder happens to author - the role WaveDefinition's own hp/price used to
    // play before this feature moved those numbers onto EnemyDefinition itself.
    private static final EnemyDefinition WEAKLING = EnemyDefinition.of("weakling", "Weakling", 1, 7, 1.28f,
            BodyArchetype.CIRCLE);

    @Test
    void loadingALevelReplacesEveryPartOfTheWorldInOneVisibleStep() {
        // The five parts of a level are correlated: the wave counter indexes the wave list,
        // the board describes the same grid the cells lay out, and the path is what marked
        // those cells unbuildable. They cross to the game-loop thread as one LoadedLevel, so a
        // reader can never pair the incoming board with the outgoing grid.
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));
        engine.nextWave();

        engine.loadLevel(LevelFixtures.biggerLevelWith(List.of(), 50));

        LoadedLevel installed = engine.getGameWorld().level();
        assertThat(installed.cells().width()).isEqualTo(20);
        assertThat(installed.board().maxX()).isEqualTo(20 * BoardFixtures.SCALE - 1);
        assertThat(installed.waveCount()).isZero();
        assertThat(installed.pathAt(0).points()).isNotEmpty();
        assertThat(engine.getCurrentWaveIndex()).isZero();
    }

    @Test
    void loadingALevelRegistersItsOwnCustomEnemiesSoItsWavesCanSpawnThem() {
        EnemyDefinition tankySquare = EnemyDefinition.of("tankySquare", "Tanky Square", 100, 5, 1.28f,
                BodyArchetype.SQUARE);
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("tankySquare", Rank.GRUNT)), 100)
                .withCustomEnemies(List.of(tankySquare)));

        assertThat(engine.getGameWorld().getEnemyCatalog().ids()).contains("tankySquare");
        assertThat(engine.nextWave()).isTrue();
        assertThat(engine.getGameWorld().enemies().getEnemies()).hasSize(1);
    }

    @Test
    void waveProgressReportsAnIndexAndCountThatBelongToTheSameLevel() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(
                new WaveDefinition("c", Rank.GRUNT),
                new WaveDefinition("s", Rank.GRUNT)), 100));

        engine.nextWave();

        WaveProgress progress = engine.waveProgress();
        assertThat(progress.index()).isEqualTo(1);
        assertThat(progress.count()).isEqualTo(2);
        assertThat(progress.hasNextWave()).isTrue();
        assertThat(progress.current()).isNotEmpty();
        assertThat(progress.next()).isNotEmpty();
    }

    @Test
    void waveProgressOnTheLastWaveOffersNoNextWave() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));

        engine.nextWave();

        WaveProgress progress = engine.waveProgress();
        assertThat(progress.hasNextWave()).isFalse();
        assertThat(progress.next()).isEmpty();
        assertThat(progress.current()).isNotEmpty();
    }

    @Test
    void waveProgressSurvivesALevelWithFewerWavesThanTheOneBeforeIt() {
        // The wave counter is per-run progress, not level state, so for a moment it can name a
        // wave the newly installed level does not have. Reporting the incoming level beats
        // indexing off the end of its shorter list.
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(
                new WaveDefinition("c", Rank.GRUNT),
                new WaveDefinition("s", Rank.GRUNT)), 100));
        engine.nextWave();

        engine.getGameWorld().installLevel(engine.getGameWorld().level().withCatalog(
                engine.getGameWorld().getEnemyCatalog()));

        assertThatCode(engine::waveProgress).doesNotThrowAnyException();
    }

    @Test
    void placingATowerOnABuildableCellChargesCreditsAndOccupiesTheCell() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        Optional<Tower> selected = engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));

        assertThat(selected).isEmpty(); // placing doesn't "select" the newly-built tower
        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(100 - SniperTower.PRICE);
        assertThat(engine.cells().at(0, 0).hasTower()).isTrue();
        assertThat(engine.isPlacingTower()).isFalse();
    }

    @Test
    void placingATowerWithoutEnoughCreditsCancelsPlacementWithoutBuildingOrCharging() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), SniperTower.PRICE - 1));

        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        Optional<Tower> selected = engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));

        assertThat(selected).isEmpty();
        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(SniperTower.PRICE - 1);
        assertThat(engine.cells().at(0, 0).hasTower()).isFalse();
        assertThat(engine.isPlacingTower()).isFalse(); // failed payment still cancels placement mode
    }

    @Test
    void clickingAnOccupiedCellSelectsItsTower() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));

        Optional<Tower> selected = engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));

        assertThat(selected).get().extracting(Tower::getType).isEqualTo(TowerFactory.Type.SNIPER);
    }

    @Test
    void placingOnAPathCellIsRejectedAndCostsNothing() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(2));

        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(100);
        assertThat(engine.cells().at(0, 2).hasTower()).isFalse();
    }

    @Test
    void towerKillsInRangeEnemyCreditsThePlayerAndReArmsTheWave() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        // enemy starts at (0,2) already within a tower placed at (2,1)'s range (SniperTower.RANGE = 3.8
        // cells) - one row off the path itself, since (2,2) is now unbuildable: the straight-line path
        // from (0,2) to (4,2) geometrically covers every cell it passes through, (2,2) included, not
        // just its two listed endpoints.
        // a 2nd wave must exist for "wave cleared" to mean "next wave ready"
        // rather than "no more waves" (game won) - see GameEngine.doTick/nextWave
        engine.loadLevel(LevelFixtures.levelWith(
                List.of(new WaveDefinition("weakling", Rank.GRUNT),
                        new WaveDefinition("weakling", Rank.GRUNT)), 100)
                .withCustomEnemies(List.of(WEAKLING)));

        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(2), BoardFixtures.cellCenter(1));
        int creditsAfterBuild = engine.getGameWorld().economy().getCredits();
        int scoreBefore = engine.getGameWorld().economy().getScore();

        assertThat(engine.nextWave()).isTrue();
        assertThat(engine.isWaveReady()).isFalse();

        for (int t = 1; t <= 10 && engine.getGameWorld().economy().getScore() == scoreBefore; t++) {
            engine.doTick(t);
        }

        assertThat(engine.getGameWorld().economy().getScore()).isEqualTo(scoreBefore + 7);
        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(creditsAfterBuild + 7);
        assertThat(engine.isWaveReady()).isTrue(); // 2nd wave exists -> ready to start it
    }

    @Test
    void aSwarmSlotIsNotClearedUntilEveryMemberDies() {
        // The highest-risk line docs/features/FEATURE-enemy-spawn-types.md calls out: a shaped
        // slot's member count, not its slot count, is what WaveContent.enemyCount() reports and
        // GameWorld.startWave seeds the roster's alive count from - a swarm of 3 must cost 3
        // kills to clear, not 1, or the wave clears early (or never).
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(
                List.of(new WaveDefinition("swarm 3 weakling", Rank.GRUNT),
                        new WaveDefinition("weakling", Rank.GRUNT)), 100)
                .withCustomEnemies(List.of(WEAKLING)));

        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(2), BoardFixtures.cellCenter(1));
        int scoreBefore = engine.getGameWorld().economy().getScore();

        assertThat(engine.nextWave()).isTrue();
        assertThat(engine.isWaveReady()).isFalse();

        int t = 0;
        for (; t <= 60 && engine.getGameWorld().economy().getScore() == scoreBefore; t++) {
            engine.doTick(t);
        }

        // one of the swarm's three members is dead - the slot is not cleared yet
        assertThat(engine.getGameWorld().economy().getScore()).isGreaterThan(scoreBefore);
        assertThat(engine.isWaveReady()).isFalse();

        for (; t <= 300 && !engine.isWaveReady(); t++) {
            engine.doTick(t);
        }

        // every member's bounty share sums to exactly one normal spawn's bounty (7) - a swarm
        // neither pays out more nor less than the definition it was built from
        assertThat(engine.getGameWorld().economy().getScore()).isEqualTo(scoreBefore + 7);
        assertThat(engine.isWaveReady()).isTrue();
    }

    @Test
    void startingARoundSpawnsBothPathsWavesInOneCall() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.twoPathLevelWith(
                List.of(new WaveDefinition("c", Rank.GRUNT)),
                List.of(new WaveDefinition("2 c", Rank.GRUNT))));

        assertThat(engine.nextWave()).isTrue();

        // 1 enemy from path A's wave, 2 from path B's - one nextWave() call spawns every path's
        // wave for the round together, into the one roster both share.
        assertThat(engine.getGameWorld().enemies().getEnemies()).hasSize(3);
    }

    @Test
    void aRoundDoesNotClearUntilEveryPathsEnemiesAreGone() {
        // Path A's one enemy sits in a tower's range and gets killed quickly; path B's one
        // enemy is on a separate row no tower here can reach, so it is only removed from the
        // roster once it leaks off the far end of its own short path. The round must stay
        // un-ready the whole time path B's enemy is still walking, even though path A's is
        // long dead - "the round is cleared" has to wait for every path, not just one.
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.twoPathLevelWith(
                List.of(new WaveDefinition("c", Rank.GRUNT), new WaveDefinition("c", Rank.GRUNT)),
                List.of(new WaveDefinition("c", Rank.GRUNT), new WaveDefinition("c", Rank.GRUNT))));
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(2), BoardFixtures.cellCenter(1)); // in range of path A's row (y=2) only
        int scoreBefore = engine.getGameWorld().economy().getScore();

        assertThat(engine.nextWave()).isTrue();

        int t = 0;
        for (; t <= 60 && engine.getGameWorld().economy().getScore() == scoreBefore; t++) {
            engine.doTick(t);
        }

        // path A's enemy is dead (credited with a kill's score); path B's is still walking its
        // own path, so the round must not be ready yet.
        assertThat(engine.getGameWorld().economy().getScore()).isGreaterThan(scoreBefore);
        assertThat(engine.isWaveReady()).isFalse();

        for (; t <= 400 && !engine.isWaveReady(); t++) {
            engine.doTick(t);
        }

        assertThat(engine.isWaveReady()).isTrue();
    }

    @Test
    void buildabilityUnionsEveryPathNotJustTheFirst() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.twoPathLevelWith(List.of(), List.of()));

        // (2, 5) sits on path B's row - path A's own coverage (row 2) never reaches it, so this
        // cell is unbuildable only because path B's coverage is unioned in too.
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        Optional<Tower> selected = engine.mouseClicked(BoardFixtures.cellCenter(2), BoardFixtures.cellCenter(5));

        assertThat(selected).isEmpty();
        assertThat(engine.cells().at(2, 5).hasTower()).isFalse();
    }

    @Test
    void aMortarShellTravelsThenSplashesAnInRangeEnemyCreditingThePlayer() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        // same placement as the SniperTower case above - well within Mortar's own (larger) range too.
        engine.loadLevel(LevelFixtures.levelWith(
                List.of(new WaveDefinition("weakling", Rank.GRUNT),
                        new WaveDefinition("weakling", Rank.GRUNT)), 100)
                .withCustomEnemies(List.of(WEAKLING)));

        engine.startPlacing(TowerFactory.Type.MORTAR, MortarTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(2), BoardFixtures.cellCenter(1));
        int creditsAfterBuild = engine.getGameWorld().economy().getCredits();
        int scoreBefore = engine.getGameWorld().economy().getScore();

        assertThat(engine.nextWave()).isTrue();

        for (int t = 1; t <= 20 && engine.getGameWorld().economy().getScore() == scoreBefore; t++) {
            engine.doTick(t);
        }

        assertThat(engine.getGameWorld().economy().getScore()).isEqualTo(scoreBefore + 7);
        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(creditsAfterBuild + 7);
        assertThat(engine.getGameWorld().projectiles().getProjectiles()).isEmpty();
    }

    @Test
    void enemyReachingTheEndOfThePathCostsALife() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));
        int initialLives = engine.getGameWorld().economy().getLives();

        engine.nextWave();
        for (int t = 1; t <= 200; t++) {
            engine.doTick(t);
        }

        assertThat(engine.getGameWorld().economy().getLives()).isLessThan(initialLives);
    }

    @Test
    void sellingATowerRefundsSeventyFivePercentAndClearsTheCell() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));
        int creditsAfterBuild = engine.getGameWorld().economy().getCredits();
        Tower placed = engine.cells().at(0, 0).getTower();

        engine.getGameWorld().towers().sell(placed);

        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(creditsAfterBuild + placed.getSellPrice());
        assertThat(engine.cells().at(0, 0).hasTower()).isFalse();
        assertThat(engine.cells().at(0, 0).buildable()).isTrue();
    }

    @Test
    void reloadingALevelRemovesTowersLeftFromThePreviousLevel() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));
        assertThat(engine.cells().at(0, 0).hasTower()).isTrue();

        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        assertThat(engine.getGameWorld().towers().all()).isEmpty();
        assertThat(engine.cells().at(0, 0).hasTower()).isFalse();
    }

    @Test
    void reloadingALevelClearsEnemiesStillAliveFromThePreviousLevel() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));
        engine.nextWave();
        assertThat(engine.getGameWorld().enemies().getEnemies()).isNotEmpty();

        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        assertThat(engine.getGameWorld().enemies().getEnemies()).isEmpty();
    }

    @Test
    void reloadingALevelClearsProjectilesStillInFlightFromThePreviousLevel() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        engine.getGameWorld().projectiles().add(new CannonballProjectile(0, 0, 1000, 0, 1f, (x, y) -> {
        }));
        assertThat(engine.getGameWorld().projectiles().getProjectiles()).isNotEmpty();

        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        assertThat(engine.getGameWorld().projectiles().getProjectiles()).isEmpty();
    }

    @Test
    void aProjectileAdvancesOnATickBetweenEnemiesAndTowersMoving() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        engine.getGameWorld().projectiles().add(new CannonballProjectile(0, 0, 100, 0, 10f, (x, y) -> {
        }));

        engine.doTick(1);

        assertThat(engine.getGameWorld().projectiles().getProjectiles().getFirst().getX()).isEqualTo(10.0);
    }

    @Test
    void reloadingALevelReseedsCreditsAndLivesFromTheNewLevel() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));

        LevelDefinition next = LevelDefinition.unsmoothed("Next", "", 5, 5, LevelFixtures.STRAIGHT_PATH, List.of(), 75, 3);
        engine.loadLevel(next);

        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(75);
        assertThat(engine.getGameWorld().economy().getLives()).isEqualTo(3);
    }

    @Test
    void reloadingALevelRewindsTheWaveCounterAndReArmsTheFirstWave() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));
        engine.nextWave();
        assertThat(engine.getCurrentWaveIndex()).isEqualTo(1);

        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));

        assertThat(engine.getCurrentWaveIndex()).isEqualTo(0);
        assertThat(engine.isWaveReady()).isTrue();
    }

    @Test
    void aWaveRequestedButNotYetStartedDoesNotCarryIntoTheNextLevel() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));
        engine.requestNextWave();

        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));

        assertThat(engine.doTick(1)).isFalse();
        assertThat(engine.getCurrentWaveIndex()).isZero();
    }

    @Test
    void loadingASmallerLevelWithACellHighlightedFromTheBiggerOneDoesNotCrash() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.biggerLevelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.highlightCell(BoardFixtures.cellCenter(18), BoardFixtures.cellCenter(14));

        assertThatCode(() -> engine.loadLevel(LevelFixtures.levelWith(List.of(), 100)))
                .doesNotThrowAnyException();
        assertThatCode(() -> engine.doTick(1)).doesNotThrowAnyException();
    }

    @Test
    void loadingASmallerLevelClearsTowersAgainstTheOldBoardRatherThanTheNewOne() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.biggerLevelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(18), BoardFixtures.cellCenter(14));
        assertThat(engine.cells().at(18, 14).hasTower()).isTrue();

        assertThatCode(() -> engine.loadLevel(LevelFixtures.levelWith(List.of(), 100)))
                .doesNotThrowAnyException();
    }

    @Test
    void debugSkippingAWaveClearsItAndStartsTheNextOneImmediately() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(
                List.of(new WaveDefinition("c", Rank.GRUNT),
                        new WaveDefinition("c", Rank.GRUNT)), 100));
        engine.nextWave();
        assertThat(engine.getCurrentWaveIndex()).isEqualTo(1);

        assertThat(engine.debugSkipCurrentWave()).isTrue();

        assertThat(engine.getCurrentWaveIndex()).isEqualTo(2);
        assertThat(engine.getGameWorld().enemies().getEnemies()).isNotEmpty(); // the 2nd wave's own enemies
        assertThat(engine.isWaveReady()).isFalse();
    }

    @Test
    void debugSkippingTheFinalWaveClearsTheBoardButStartsNothing() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));
        engine.nextWave();
        assertThat(engine.getGameWorld().enemies().getEnemies()).isNotEmpty();

        assertThat(engine.debugSkipCurrentWave()).isFalse();

        assertThat(engine.getGameWorld().enemies().getEnemies()).isEmpty();
        assertThat(engine.getCurrentWaveIndex()).isEqualTo(1);
    }

    @Test
    void debugSkippingAWaveCostsNoLivesAndPaysNoCredits() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(new WaveDefinition("c", Rank.GRUNT)), 100));
        engine.nextWave();
        int livesBefore = engine.getGameWorld().economy().getLives();
        int creditsBefore = engine.getGameWorld().economy().getCredits();

        engine.debugSkipCurrentWave();

        assertThat(engine.getGameWorld().economy().getLives()).isEqualTo(livesBefore);
        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(creditsBefore);
    }

    @Test
    void debugSpawnCyclesThroughEveryCatalogIdInOrderThenWraps() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        List<String> ids = engine.getGameWorld().getEnemyCatalog().ids();

        for (String expected : ids) {
            assertThat(engine.debugSpawnNextCatalogEnemy()).contains(expected);
        }
        assertThat(engine.debugSpawnNextCatalogEnemy()).contains(ids.getFirst()); // wraps around
    }

    @Test
    void debugSpawnAddsALiveEnemyToTheRosterImmediately() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        engine.debugSpawnNextCatalogEnemy();

        assertThat(engine.getGameWorld().enemies().getEnemies()).hasSize(1);
    }

    @Test
    void debugSpawnWithNoLevelLoadedSpawnsNothing() {
        GameEngine engine = FakeGameHost.newBoundEngine();

        assertThat(engine.debugSpawnNextCatalogEnemy()).isEmpty();
    }

    @Test
    void debugGrantingCreditsRaisesTheBalanceByExactlyTheAmount() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        engine.debugGrantCredits(250);

        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(350);
    }
}
