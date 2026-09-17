package td;

import org.junit.jupiter.api.Test;
import td.level.LevelDefinition;
import td.projectile.CannonballProjectile;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.TowerMortar;
import td.tower.TowerOne;
import td.wave.Point;
import td.wave.WaveDefinition;

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

    private static final int SCALE = 32; // GameWorld's default scale, unless a test changes it
    // straight path along row y=2; also the path waypoint PathNormal.finalise() marks unbuildable
    private static final List<Point> STRAIGHT_PATH = List.of(new Point(0, 2), new Point(4, 2));

    private static LevelDefinition levelWith(List<WaveDefinition> waves, int startingCredits) {
        return LevelDefinition.unsmoothed("Test Level", "", 5, 5, STRAIGHT_PATH, waves, startingCredits, 5);
    }

    private static LevelDefinition biggerLevelWith(List<WaveDefinition> waves, int startingCredits) {
        return LevelDefinition.unsmoothed("Bigger Level", "", 20, 15, STRAIGHT_PATH, waves, startingCredits, 5);
    }

    @Test
    void placingATowerOnABuildableCellChargesCreditsAndOccupiesTheCell() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(), 100));

        engine.startPlacing(TowerFactory.Type.first, TowerOne.RANGE);
        Optional<Tower> selected = engine.mouseClicked(cellCenter(0), cellCenter(0));

        assertThat(selected).isEmpty(); // placing doesn't "select" the newly-built tower
        assertThat(engine.getGameWorld().getCredits()).isEqualTo(100 - TowerOne.PRICE);
        assertThat(engine.cells().at(0, 0).hasTower()).isTrue();
        assertThat(engine.isPlacingTower()).isFalse();
    }

    @Test
    void placingATowerWithoutEnoughCreditsCancelsPlacementWithoutBuildingOrCharging() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(), TowerOne.PRICE - 1));

        engine.startPlacing(TowerFactory.Type.first, TowerOne.RANGE);
        Optional<Tower> selected = engine.mouseClicked(cellCenter(0), cellCenter(0));

        assertThat(selected).isEmpty();
        assertThat(engine.getGameWorld().getCredits()).isEqualTo(TowerOne.PRICE - 1);
        assertThat(engine.cells().at(0, 0).hasTower()).isFalse();
        assertThat(engine.isPlacingTower()).isFalse(); // failed payment still cancels placement mode
    }

    @Test
    void clickingAnOccupiedCellSelectsItsTower() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.first, TowerOne.RANGE);
        engine.mouseClicked(cellCenter(0), cellCenter(0));

        Optional<Tower> selected = engine.mouseClicked(cellCenter(0), cellCenter(0));

        assertThat(selected).get().extracting(Tower::getType).isEqualTo(TowerFactory.Type.first);
    }

    @Test
    void placingOnAPathCellIsRejectedAndCostsNothing() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(), 100));

        engine.startPlacing(TowerFactory.Type.first, TowerOne.RANGE);
        engine.mouseClicked(cellCenter(0), cellCenter(2));

        assertThat(engine.getGameWorld().getCredits()).isEqualTo(100);
        assertThat(engine.cells().at(0, 2).hasTower()).isFalse();
    }

    @Test
    void towerKillsInRangeEnemyCreditsThePlayerAndReArmsTheWave() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        // enemy starts at (0,2) already within a tower placed at (2,1)'s range (TowerOne.RANGE = 3.8
        // cells) - one row off the path itself, since (2,2) is now unbuildable: the straight-line path
        // from (0,2) to (4,2) geometrically covers every cell it passes through, (2,2) included, not
        // just its two listed endpoints.
        // a 2nd wave must exist for "wave cleared" to mean "next wave ready"
        // rather than "no more waves" (game won) - see GameEngine.doTick/nextWave
        engine.loadLevel(levelWith(
                List.of(new WaveDefinition("c", 1, 7, 1),
                        new WaveDefinition("c", 1, 7, 1)), 100));

        engine.startPlacing(TowerFactory.Type.first, TowerOne.RANGE);
        engine.mouseClicked(cellCenter(2), cellCenter(1));
        int creditsAfterBuild = engine.getGameWorld().getCredits();
        int scoreBefore = engine.getGameWorld().getScore();

        assertThat(engine.nextWave()).isTrue();
        assertThat(engine.isWaveReady()).isFalse();

        for (int t = 1; t <= 10 && engine.getGameWorld().getScore() == scoreBefore; t++) {
            engine.doTick(t);
        }

        assertThat(engine.getGameWorld().getScore()).isEqualTo(scoreBefore + 7);
        assertThat(engine.getGameWorld().getCredits()).isEqualTo(creditsAfterBuild + 7);
        assertThat(engine.isWaveReady()).isTrue(); // 2nd wave exists -> ready to start it
    }

    @Test
    void aMortarShellTravelsThenSplashesAnInRangeEnemyCreditingThePlayer() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        // same placement as the TowerOne case above - well within Mortar's own (larger) range too.
        engine.loadLevel(levelWith(
                List.of(new WaveDefinition("c", 1, 7, 1),
                        new WaveDefinition("c", 1, 7, 1)), 100));

        engine.startPlacing(TowerFactory.Type.mortar, TowerMortar.RANGE);
        engine.mouseClicked(cellCenter(2), cellCenter(1));
        int creditsAfterBuild = engine.getGameWorld().getCredits();
        int scoreBefore = engine.getGameWorld().getScore();

        assertThat(engine.nextWave()).isTrue();

        for (int t = 1; t <= 20 && engine.getGameWorld().getScore() == scoreBefore; t++) {
            engine.doTick(t);
        }

        assertThat(engine.getGameWorld().getScore()).isEqualTo(scoreBefore + 7);
        assertThat(engine.getGameWorld().getCredits()).isEqualTo(creditsAfterBuild + 7);
        assertThat(engine.getGameWorld().getProjectileRegistry().getProjectiles()).isEmpty();
    }

    @Test
    void enemyReachingTheEndOfThePathCostsALife() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(new WaveDefinition("c", 100, 3, 1)), 100));
        int initialLives = engine.getGameWorld().getLives();

        engine.nextWave();
        for (int t = 1; t <= 200; t++) {
            engine.doTick(t);
        }

        assertThat(engine.getGameWorld().getLives()).isLessThan(initialLives);
    }

    @Test
    void sellingATowerRefundsSeventyFivePercentAndClearsTheCell() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.first, TowerOne.RANGE);
        engine.mouseClicked(cellCenter(0), cellCenter(0));
        int creditsAfterBuild = engine.getGameWorld().getCredits();
        Tower placed = engine.cells().at(0, 0).getTower();

        engine.getGameWorld().sellTower(placed);

        assertThat(engine.getGameWorld().getCredits()).isEqualTo(creditsAfterBuild + placed.getSellPrice());
        assertThat(engine.cells().at(0, 0).hasTower()).isFalse();
        assertThat(engine.cells().at(0, 0).buildable()).isTrue();
    }

    @Test
    void reloadingALevelRemovesTowersLeftFromThePreviousLevel() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.first, TowerOne.RANGE);
        engine.mouseClicked(cellCenter(0), cellCenter(0));
        assertThat(engine.cells().at(0, 0).hasTower()).isTrue();

        engine.loadLevel(levelWith(List.of(), 100));

        assertThat(engine.getGameWorld().getTowers()).isEmpty();
        assertThat(engine.cells().at(0, 0).hasTower()).isFalse();
    }

    @Test
    void reloadingALevelClearsEnemiesStillAliveFromThePreviousLevel() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(new WaveDefinition("c", 1000, 3, 1)), 100));
        engine.nextWave();
        assertThat(engine.getGameWorld().getEnemies()).isNotEmpty();

        engine.loadLevel(levelWith(List.of(), 100));

        assertThat(engine.getGameWorld().getEnemies()).isEmpty();
    }

    @Test
    void reloadingALevelClearsProjectilesStillInFlightFromThePreviousLevel() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(), 100));
        engine.getGameWorld().addProjectile(new CannonballProjectile(0, 0, 1000, 0, 1f, (x, y) -> {
        }));
        assertThat(engine.getGameWorld().getProjectileRegistry().getProjectiles()).isNotEmpty();

        engine.loadLevel(levelWith(List.of(), 100));

        assertThat(engine.getGameWorld().getProjectileRegistry().getProjectiles()).isEmpty();
    }

    @Test
    void aProjectileAdvancesOnATickBetweenEnemiesAndTowersMoving() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(), 100));
        engine.getGameWorld().addProjectile(new CannonballProjectile(0, 0, 100, 0, 10f, (x, y) -> {
        }));

        engine.doTick(1);

        assertThat(engine.getGameWorld().getProjectileRegistry().getProjectiles().get(0).getX()).isEqualTo(10.0);
    }

    @Test
    void reloadingALevelReseedsCreditsAndLivesFromTheNewLevel() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.first, TowerOne.RANGE);
        engine.mouseClicked(cellCenter(0), cellCenter(0));

        LevelDefinition next = LevelDefinition.unsmoothed("Next", "", 5, 5, STRAIGHT_PATH, List.of(), 75, 3);
        engine.loadLevel(next);

        assertThat(engine.getGameWorld().getCredits()).isEqualTo(75);
        assertThat(engine.getGameWorld().getLives()).isEqualTo(3);
    }

    @Test
    void reloadingALevelRewindsTheWaveCounterAndReArmsTheFirstWave() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(new WaveDefinition("c", 1, 3, 1)), 100));
        engine.nextWave();
        assertThat(engine.getCurrentWaveIndex()).isEqualTo(1);

        engine.loadLevel(levelWith(List.of(new WaveDefinition("c", 1, 3, 1)), 100));

        assertThat(engine.getCurrentWaveIndex()).isEqualTo(0);
        assertThat(engine.isWaveReady()).isTrue();
    }

    @Test
    void aWaveRequestedButNotYetStartedDoesNotCarryIntoTheNextLevel() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(new WaveDefinition("c", 1, 3, 1)), 100));
        engine.requestNextWave();

        engine.loadLevel(levelWith(List.of(new WaveDefinition("c", 1, 3, 1)), 100));

        assertThat(engine.doTick(1)).isFalse();
        assertThat(engine.getCurrentWaveIndex()).isZero();
    }

    @Test
    void loadingASmallerLevelWithACellHighlightedFromTheBiggerOneDoesNotCrash() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(biggerLevelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.first, TowerOne.RANGE);
        engine.highlightCell(cellCenter(18), cellCenter(14));

        assertThatCode(() -> engine.loadLevel(levelWith(List.of(), 100)))
                .doesNotThrowAnyException();
        assertThatCode(() -> engine.doTick(1)).doesNotThrowAnyException();
    }

    @Test
    void loadingASmallerLevelClearsTowersAgainstTheOldBoardRatherThanTheNewOne() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(biggerLevelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.Type.first, TowerOne.RANGE);
        engine.mouseClicked(cellCenter(18), cellCenter(14));
        assertThat(engine.cells().at(18, 14).hasTower()).isTrue();

        assertThatCode(() -> engine.loadLevel(levelWith(List.of(), 100)))
                .doesNotThrowAnyException();
    }

    @Test
    void debugSkippingAWaveClearsItAndStartsTheNextOneImmediately() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(
                List.of(new WaveDefinition("c", 100, 3, 1),
                        new WaveDefinition("c", 100, 3, 1)), 100));
        engine.nextWave();
        assertThat(engine.getCurrentWaveIndex()).isEqualTo(1);

        assertThat(engine.debugSkipCurrentWave()).isTrue();

        assertThat(engine.getCurrentWaveIndex()).isEqualTo(2);
        assertThat(engine.getGameWorld().getEnemies()).isNotEmpty(); // the 2nd wave's own enemies
        assertThat(engine.isWaveReady()).isFalse();
    }

    @Test
    void debugSkippingTheFinalWaveClearsTheBoardButStartsNothing() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(new WaveDefinition("c", 100, 3, 1)), 100));
        engine.nextWave();
        assertThat(engine.getGameWorld().getEnemies()).isNotEmpty();

        assertThat(engine.debugSkipCurrentWave()).isFalse();

        assertThat(engine.getGameWorld().getEnemies()).isEmpty();
        assertThat(engine.getCurrentWaveIndex()).isEqualTo(1);
    }

    @Test
    void debugSkippingAWaveCostsNoLivesAndPaysNoCredits() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(new WaveDefinition("c", 100, 7, 1)), 100));
        engine.nextWave();
        int livesBefore = engine.getGameWorld().getLives();
        int creditsBefore = engine.getGameWorld().getCredits();

        engine.debugSkipCurrentWave();

        assertThat(engine.getGameWorld().getLives()).isEqualTo(livesBefore);
        assertThat(engine.getGameWorld().getCredits()).isEqualTo(creditsBefore);
    }

    @Test
    void debugSpawnCyclesThroughEveryCatalogIdInOrderThenWraps() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(), 100));
        List<String> ids = engine.getGameWorld().getEnemyCatalog().ids();

        for (String expected : ids) {
            assertThat(engine.debugSpawnNextCatalogEnemy()).contains(expected);
        }
        assertThat(engine.debugSpawnNextCatalogEnemy()).contains(ids.get(0)); // wraps around
    }

    @Test
    void debugSpawnAddsALiveEnemyToTheRosterImmediately() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(), 100));

        engine.debugSpawnNextCatalogEnemy();

        assertThat(engine.getGameWorld().getEnemies()).hasSize(1);
    }

    @Test
    void debugSpawnWithNoLevelLoadedSpawnsNothing() {
        GameEngine engine = FakeGameHost.newBoundEngine();

        assertThat(engine.debugSpawnNextCatalogEnemy()).isEmpty();
    }

    @Test
    void debugGrantingCreditsRaisesTheBalanceByExactlyTheAmount() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(), 100));

        engine.debugGrantCredits(250);

        assertThat(engine.getGameWorld().getCredits()).isEqualTo(350);
    }

    private static int cellCenter(int cellIndex) {
        return cellIndex * SCALE + SCALE / 2;
    }
}
