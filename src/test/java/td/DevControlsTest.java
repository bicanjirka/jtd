package td;

import org.junit.jupiter.api.Test;
import td.enemy.Rank;
import td.fixtures.BoardFixtures;
import td.fixtures.LevelFixtures;
import td.level.LevelOutcome;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;
import td.util.GameWorld;
import td.wave.WaveDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DevControlsTest {

    private final GameEngine engine = FakeGameHost.newBoundEngine();
    private final GameWorld world = this.engine.getGameWorld();
    private final DevControls dev = new DevControls(this.world);

    DevControlsTest() {
        this.world.economy().addEconomyListener(this.dev);
    }

    @Test
    void creditsAndLivesAreSetToExactlyWhatWasTyped() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        this.dev.setCredits(4321);
        this.dev.setLives(42);

        assertThat(this.world.economy().getCredits()).isEqualTo(4321);
        assertThat(this.world.economy().getLives()).isEqualTo(42);
    }

    @Test
    void withInfiniteLivesALeakOnTheLastLifeComesBackAndTheLevelGoesOn() {
        this.engine.loadLevel(LevelFixtures.biggerLevelWith(
                List.of(new WaveDefinition("c", Rank.GRUNT), new WaveDefinition("c", Rank.GRUNT)), 100)
                .withStartingLives(1));
        this.dev.setInfiniteLives(true);
        this.engine.nextWave();

        tickUntilClear();

        assertThat(this.engine.outcome()).isEqualTo(LevelOutcome.PLAYING);
        assertThat(this.world.economy().getLives()).isEqualTo(1);
    }

    @Test
    void freeBuildKeepsCreditsAtAMillionThroughAPurchase() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        this.dev.setFreeBuild(true);

        Tower tower = this.placeSniper(1, 1);

        assertThat(tower).isNotNull();
        assertThat(this.world.economy().getCredits()).isEqualTo(DevControls.FREE_BUILD_CREDITS);
    }

    @Test
    void aSpawnedScriptJoinsTheBoardOnlyWhenTheGameLoopRunsIt() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        String message = this.dev.spawnWave("3 c", Rank.GRUNT, 0);
        int beforeRun = this.world.enemies().getEnemies().length;
        this.dev.runPending();

        assertThat(message).isEqualTo("Spawning 3 on path 1");
        assertThat(beforeRun).isZero();
        assertThat(this.world.enemies().aliveCount()).isEqualTo(3);
    }

    @Test
    void aScriptThatDoesNotParseSpawnsNothingAndSaysWhy() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));

        String message = this.dev.spawnWave("3 nosuchenemy", Rank.GRUNT, 0);
        this.dev.runPending();

        assertThat(message).contains("nosuchenemy");
        assertThat(this.world.enemies().getEnemies()).isEmpty();
    }

    @Test
    void killAllPaysEachEnemysBountyAndXpLikeARealKill() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        Tower tower = this.placeSniper(1, 1);
        this.dev.spawnWave("c", Rank.GRUNT, 0);
        this.dev.runPending();
        int bounty = this.world.enemies().getEnemies()[0].getBounty();
        int creditsBefore = this.world.economy().getCredits();

        this.dev.killAll();
        this.dev.runPending();

        assertThat(this.world.enemies().aliveCount()).isZero();
        assertThat(this.world.economy().getCredits()).isEqualTo(creditsBefore + bounty);
        assertThat(tower.experience().xp()).isEqualTo(bounty);
    }

    @Test
    void clearAllRemovesEveryEnemyAndPaysNothing() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        this.dev.spawnWave("4 c", Rank.GRUNT, 0);
        this.dev.runPending();
        int creditsBefore = this.world.economy().getCredits();

        this.dev.clearAll();
        this.dev.runPending();

        assertThat(this.world.enemies().getEnemies()).isEmpty();
        assertThat(this.world.economy().getCredits()).isEqualTo(creditsBefore);
    }

    @Test
    void xpGoesToTheSelectedTowerAndNeedsOne() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 100));
        Tower tower = this.placeSniper(1, 1);
        String withoutSelection = this.dev.grantXpToSelected(100);
        tower.setSelected(true);

        String withSelection = this.dev.grantXpToSelected(100);
        this.dev.runPending();

        assertThat(withoutSelection).isEqualTo("Select a tower first");
        assertThat(withSelection).isEqualTo("+100 XP to SNIPER");
        assertThat(tower.experience().xp()).isEqualTo(100);
    }

    @Test
    void ignoringGatesLetsATowerBuyANodeBeforeItHasTheXp() {
        this.engine.loadLevel(LevelFixtures.levelWith(List.of(), 1000));
        Tower tower = this.placeSniper(1, 1);
        tower.buyUpgrade(named(tower, "Attune"));
        tower.buyUpgrade(named(tower, "Focused Optics"));
        boolean gated = tower.buyUpgrade(named(tower, "Focused Optics II"));

        this.dev.setUpgradeGatesIgnored(true);
        boolean ignored = tower.buyUpgrade(named(tower, "Focused Optics II"));

        assertThat(gated).isFalse();
        assertThat(ignored).isTrue();
    }

    private Tower placeSniper(int cellX, int cellY) {
        this.engine.startPlacing(TowerFactory.Type.SNIPER, 0f);
        this.engine.mouseClicked(BoardFixtures.cellCenter(cellX), BoardFixtures.cellCenter(cellY));
        return this.engine.cells().at(cellX, cellY).getTower();
    }

    private void tickUntilClear() {
        for (int t = 1; t < 2000 && this.world.enemies().aliveCount() > 0; t++) {
            this.engine.doTick(t);
        }
    }

    private static UpgradeNode named(Tower tower, String name) {
        return tower.upgradeTree().nodes().stream().filter(n -> n.displayName().equals(name)).findFirst().orElseThrow();
    }
}
