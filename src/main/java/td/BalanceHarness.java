package td;

import td.cell.CellGrid;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameHost;
import td.util.GameWorld;
import td.util.RandomSource;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.List;

/**
 * Headless batch balance simulation: drives a {@link GameEngine} through a level with a fixed
 * tower loadout and no human input, then reports the numbers a balance decision needs - lives
 * lost, ticks-to-clear per wave, and per-tower damage/kills. Lives in {@code src/main/java}
 * (not test scope), since it is a tool run on demand rather than a regression test - see
 * {@code docs/features/FEATURE-playtesting-and-balance-tooling.md}'s V1 Scope.
 * <p>
 * Implements {@link GameHost} itself rather than using {@link GameHost#noOp()}: the no-op
 * host never re-arms {@code waveReady} (only {@code TowerDefense.enemyDied} does that, and
 * {@code GameWorld} exposes no alive-count accessor to poll instead), so this class exercises
 * the exact same callback the real game advances waves on.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
// a headless harness driven start to finish by whichever thread calls run()
public final class BalanceHarness implements GameHost {

    /**
     * Fixed so two runs of the same loadout are comparable. Randomness in the simulation
     * (SplashTower picking its primary target) would otherwise come from the unseeded global
     * {@code Math.random()} and make every run a different experiment - see
     * {@link RandomSource}. Change it deliberately to sample a different sequence.
     */
    private static final long RANDOM_SEED = 20260917L;

    private final GameEngine engine = new GameEngine(this, RandomSource.seeded(RANDOM_SEED));
    private final List<Integer> ticksToClearPerWave = new ArrayList<>();
    private int waveStartTick = 0;
    private boolean waveJustCleared = false;

    /**
     * Runs one built-in loadout against Classic Loop, so this class is executable with no arguments.
     */
    public static void main(String[] args) {
        LevelDefinition classicLoop = new BuiltInLevelCatalog().levels().get(0);
        List<TowerPlacementSpec> loadout = List.of(
                TowerPlacementSpec.of(TowerFactory.Type.SNIPER, 6, 11),
                TowerPlacementSpec.of(TowerFactory.Type.SNIPER, 8, 9),
                TowerPlacementSpec.of(TowerFactory.Type.SPLASH, 5, 3));
        new BalanceHarness().run(classicLoop, loadout, 5000);
    }

    @Override
    public void enemyDied(int enemiesLeft) {
        if (enemiesLeft == 0) {
            this.engine.setWaveReady(true); // mirrors TowerDefense.enemyDied - GameHost.noOp() never does this
            this.waveJustCleared = true;
        }
    }

    @Override
    public void setInfoText(String s) {
        // no UI to inform
    }

    @Override
    public void clearCell(int x, int y) {
        this.engine.clearCell(x, y);
    }

    /**
     * Places every {@code loadout} tower, then runs the tick loop until every wave has
     * cleared, the player runs out of lives, or {@code tickBudget} ticks pass - whichever
     * comes first - printing the report at the end either way.
     */
    public void run(LevelDefinition level, List<TowerPlacementSpec> loadout, int tickBudget) {
        this.engine.loadLevel(level);
        this.placeLoadout(loadout, this.engine.getGameWorld().getBoard().scale());

        int t = 1;
        for (; t <= tickBudget; t++) {
            if (this.engine.isWaveReady() && this.engine.getCurrentWaveIndex() < this.engine.getWaveCount()) {
                this.waveStartTick = t;
                this.engine.nextWave();
            }
            this.engine.doTick(t);
            if (this.waveJustCleared) {
                this.ticksToClearPerWave.add(t - this.waveStartTick);
                this.waveJustCleared = false;
            }
            if (this.engine.getGameWorld().economy().getLives() <= 0) {
                break;
            }
            if (this.engine.getCurrentWaveIndex() >= this.engine.getWaveCount()
                    && this.engine.getGameWorld().enemies().getEnemies().length == 0) {
                break;
            }
        }

        this.printReport(level, t);
    }

    /**
     * Places each tower through the real input surface (start placing, click the cell's
     * pixel centre) rather than reaching into engine state directly - the same path
     * {@code TowerDefense}'s mouse listener drives. {@code TowerPlacement.mouseClicked}
     * always leaves placement mode whether or not it actually built anything, so success is
     * verified afterward via the cell grid rather than trusted from a return value.
     */
    private void placeLoadout(List<TowerPlacementSpec> loadout, int scale) {
        CellGrid grid = this.engine.cells();
        for (TowerPlacementSpec spec : loadout) {
            this.engine.startPlacing(spec.type(), 0f);
            int pixelX = spec.cellX() * scale + scale / 2;
            int pixelY = spec.cellY() * scale + scale / 2;
            this.engine.mouseClicked(pixelX, pixelY);
            if (!grid.at(spec.cellX(), spec.cellY()).hasTower()) {
                System.err.println("Placement rejected, cell not buildable or unaffordable: " + spec);
            }
        }
    }

    private void printReport(LevelDefinition level, int ticksRun) {
        GameWorld gameWorld = this.engine.getGameWorld();
        int livesLost = level.startingLives() - gameWorld.economy().getLives();
        System.out.println("=== Balance report: " + level.name() + " (" + ticksRun + " ticks) ===");
        System.out.println("Lives lost: " + livesLost + " / " + level.startingLives());
        System.out.println("Waves cleared: " + this.ticksToClearPerWave.size() + " / " + this.engine.getWaveCount());
        for (int i = 0; i < this.ticksToClearPerWave.size(); i++) {
            System.out.println("  Wave " + (i + 1) + ": " + this.ticksToClearPerWave.get(i) + " ticks");
        }
        System.out.println("Per-tower stats:");
        for (Tower tower : this.engine.getTowers()) {
            System.out.println("  " + tower.getType() + " @ (" + tower.getBoardX() + "," + tower.getBoardY() + "): "
                    + tower.getKillCount() + " kills, " + tower.getDamageDealt() + " damage dealt");
        }
    }
}
