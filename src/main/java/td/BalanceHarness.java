package td;

import td.cell.CellGrid;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;
import td.level.LevelOutcome;
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
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
public final class BalanceHarness {

    /**
     * Fixed so two runs of the same loadout are comparable; change it to sample a different
     * sequence.
     */
    private static final long RANDOM_SEED = 20260917L;

    private final GameEngine engine = new GameEngine(GameHost.noOp(), RandomSource.seeded(RANDOM_SEED));
    private final List<Integer> ticksToClearPerWave = new ArrayList<>();

    /** Runs one built-in loadout, so the class is executable with no arguments. */
    public static void main(String[] args) {
        LevelDefinition curlyPath = new BuiltInLevelCatalog().levels().getFirst();
        List<TowerPlacementSpec> loadout = List.of(
                TowerPlacementSpec.of(TowerFactory.Type.SNIPER, 4, 10),
                TowerPlacementSpec.of(TowerFactory.Type.SNIPER, 11, 8),
                TowerPlacementSpec.of(TowerFactory.Type.SPLASH, 15, 9));
        new BalanceHarness().run(curlyPath, loadout, 5000);
    }

    /**
     * Places the loadout, then ticks until every wave clears, lives run out or {@code tickBudget}
     * passes, and prints the report.
     */
    public void run(LevelDefinition level, List<TowerPlacementSpec> loadout, int tickBudget) {
        this.engine.loadLevel(level);
        this.placeLoadout(loadout, this.engine.getGameWorld().getBoard().scale());

        int t = 1;
        int waveStartTick = 0;
        for (; t <= tickBudget && !this.engine.outcome().isOver(); t++) {
            if (this.engine.nextWave()) {
                waveStartTick = t;
            }
            this.engine.doTick(t);
            if (this.engine.isWaveReady() || this.engine.outcome() == LevelOutcome.WON) {
                this.ticksToClearPerWave.add(t - waveStartTick);
            }
        }

        this.printReport(level, t - 1);
    }

    /**
     * Places through the real input path. A click always leaves placement mode, so success is
     * checked on the cell grid afterwards. A rejected placement aborts the run: a report on a
     * partial loadout silently measures a different game.
     */
    private void placeLoadout(List<TowerPlacementSpec> loadout, int scale) {
        CellGrid grid = this.engine.cells();
        for (TowerPlacementSpec spec : loadout) {
            this.engine.startPlacing(spec.type(), 0f);
            int pixelX = spec.cellX() * scale + scale / 2;
            int pixelY = spec.cellY() * scale + scale / 2;
            this.engine.mouseClicked(pixelX, pixelY);
            if (!grid.at(spec.cellX(), spec.cellY()).hasTower()) {
                throw new IllegalArgumentException("Placement rejected, cell not buildable or unaffordable: " + spec);
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
