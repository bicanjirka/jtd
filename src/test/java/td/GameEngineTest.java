package td;

import org.junit.jupiter.api.Test;
import td.level.LevelDefinition;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.TowerOne;
import td.wave.Point;
import td.wave.WaveDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void placingATowerOnABuildableCellChargesCreditsAndOccupiesTheCell() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(), 100));

        engine.startPlacing(TowerFactory.type.first, TowerOne.range);
        Tower selected = engine.mouseClicked(cellCenter(0), cellCenter(0));

        assertThat(selected).isNull(); // placing doesn't "select" the newly-built tower
        assertThat(engine.getGameWorld().getCredits()).isEqualTo(100 - TowerOne.price);
        assertThat(engine.getCellGrid()[0][0].hasTower()).isTrue();
        assertThat(engine.isPlacingTower()).isFalse();
    }

    @Test
    void placingATowerWithoutEnoughCreditsCancelsPlacementWithoutBuildingOrCharging() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(), TowerOne.price - 1));

        engine.startPlacing(TowerFactory.type.first, TowerOne.range);
        Tower selected = engine.mouseClicked(cellCenter(0), cellCenter(0));

        assertThat(selected).isNull();
        assertThat(engine.getGameWorld().getCredits()).isEqualTo(TowerOne.price - 1);
        assertThat(engine.getCellGrid()[0][0].hasTower()).isFalse();
        assertThat(engine.isPlacingTower()).isFalse(); // failed payment still cancels placement mode
    }

    @Test
    void clickingAnOccupiedCellSelectsItsTower() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(), 100));
        engine.startPlacing(TowerFactory.type.first, TowerOne.range);
        engine.mouseClicked(cellCenter(0), cellCenter(0));

        Tower selected = engine.mouseClicked(cellCenter(0), cellCenter(0));

        assertThat(selected).isNotNull();
        assertThat(selected.getType()).isEqualTo(TowerFactory.type.first);
    }

    @Test
    void placingOnAPathCellIsRejectedAndCostsNothing() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(levelWith(List.of(), 100));

        engine.startPlacing(TowerFactory.type.first, TowerOne.range);
        engine.mouseClicked(cellCenter(0), cellCenter(2));

        assertThat(engine.getGameWorld().getCredits()).isEqualTo(100);
        assertThat(engine.getCellGrid()[0][2].hasTower()).isFalse();
    }

    @Test
    void towerKillsInRangeEnemyCreditsThePlayerAndReArmsTheWave() {
        GameEngine engine = FakeGameHost.newBoundEngine();
        // enemy starts at (0,2) already within a tower placed at (2,1)'s range (TowerOne.range = 3.8
        // cells) - one row off the path itself, since (2,2) is now unbuildable: the straight-line path
        // from (0,2) to (4,2) geometrically covers every cell it passes through, (2,2) included, not
        // just its two listed endpoints.
        // a 2nd wave must exist for "wave cleared" to mean "next wave ready"
        // rather than "no more waves" (game won) - see GameEngine.doTick/nextWave
        engine.loadLevel(levelWith(
                List.of(new WaveDefinition("c", 1, 7, 1),
                        new WaveDefinition("c", 1, 7, 1)), 100));

        engine.startPlacing(TowerFactory.type.first, TowerOne.range);
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
        engine.startPlacing(TowerFactory.type.first, TowerOne.range);
        engine.mouseClicked(cellCenter(0), cellCenter(0));
        int creditsAfterBuild = engine.getGameWorld().getCredits();
        Tower placed = engine.getCellGrid()[0][0].getTower();

        engine.getGameWorld().sellTower(placed);

        assertThat(engine.getGameWorld().getCredits()).isEqualTo(creditsAfterBuild + placed.getSellPrice());
        assertThat(engine.getCellGrid()[0][0].hasTower()).isFalse();
        assertThat(engine.getCellGrid()[0][0].buildable()).isTrue();
    }

    private static int cellCenter(int cellIndex) {
        return cellIndex * SCALE + SCALE / 2;
    }
}
