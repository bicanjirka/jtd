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
 * Headless balance run: plays a level with a fixed tower loadout and no input, then reports lives
 * lost, ticks to clear each wave, and per-tower damage and kills. A tool run on demand, not a test.
 * <p>
 * Implements {@link GameHost} rather than using {@link GameHost#noOp()}, because only a real host
 * re-arms the next wave when the last enemy dies.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
public final class BalanceHarness implements GameHost {

    /**
     * Fixed so two runs of the same loadout are comparable; change it to sample a different
     * sequence.
     */
    private static final long RANDOM_SEED = 20260917L;

    private final GameEngine engine = new GameEngine(this, RandomSource.seeded(RANDOM_SEED));
    private final List<Integer> ticksToClearPerWave = new ArrayList<>();
    private int waveStartTick = 0;
    private boolean waveJustCleared = false;

    /** Runs one built-in loadout, so the class is executable with no arguments. */
    public static void main(String[] args) {
        LevelDefinition curlyPath = new BuiltInLevelCatalog().levels().getFirst();
        List<TowerPlacementSpec> loadout = List.of(
                TowerPlacementSpec.of(TowerFactory.Type.SNIPER, 4, 10),
                TowerPlacementSpec.of(TowerFactory.Type.SNIPER, 10, 8),
                TowerPlacementSpec.of(TowerFactory.Type.SPLASH, 16, 9));
        new BalanceHarness().run(curlyPath, loadout, 5000);
    }

    @Override
    public void enemyDied(int enemiesLeft) {
        if (enemiesLeft == 0) {
            this.engine.setWaveReady(true); // GameHost.noOp() never marks the wave ready
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
     * Places the loadout, then ticks until every wave clears, lives run out or {@code tickBudget}
     * passes, and prints the report.
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
                    && this.engine.getGameWorld().enemies().aliveCount() == 0) {
                break;
            }
        }

        this.printReport(level, t);
    }

    /**
     * Places through the real input path. A click always leaves placement mode, so success is
     * checked on the cell grid afterwards.
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
