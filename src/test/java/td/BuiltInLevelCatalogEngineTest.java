package td;

import org.junit.jupiter.api.Test;
import td.economy.EconomyState;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;
import td.level.LevelOutcome;
import td.util.RandomSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Loads and plays every built-in level through the engine: what every level must do, whatever its numbers. */
class BuiltInLevelCatalogEngineTest {

    private static final int UNLOSABLE_LIVES = 1_000_000;
    private static final int TICK_BUDGET = 500_000;
    private static final long RANDOM_SEED = 20260924L;

    /**
     * Plays every wave of every built-in level with no towers and lives that cannot run out, so
     * each wave script spawns and walks its whole path whatever the balance numbers are.
     */
    @Test
    void everyBuiltInLevelPlaysThroughEveryWaveToAWinWhenLivesCannotRunOut() {
        for (LevelDefinition builtIn : new BuiltInLevelCatalog().levels()) {
            LevelDefinition level = builtIn.withStartingLives(UNLOSABLE_LIVES);
            GameEngine engine = FakeGameHost.newBoundEngine(RandomSource.seeded(RANDOM_SEED));
            engine.loadLevel(level);
            boolean livesEverRose = false;
            int lowestCredits = level.startingCredits();

            int t = 1;
            for (int previousLives = UNLOSABLE_LIVES; t <= TICK_BUDGET && !engine.outcome().isOver(); t++) {
                engine.nextWave();
                engine.doTick(t);
                EconomyState state = engine.getGameWorld().economy().state();
                livesEverRose |= state.lives() > previousLives;
                lowestCredits = Math.min(lowestCredits, state.credits());
                previousLives = state.lives();
            }

            assertThat(engine.outcome()).describedAs("level '%s' after %d ticks", level.name(), t)
                    .isEqualTo(LevelOutcome.WON);
            assertThat(engine.getCurrentWaveIndex()).isEqualTo(engine.getWaveCount());
            assertThat(livesEverRose).describedAs("level '%s' lives rose", level.name()).isFalse();
            assertThat(lowestCredits).describedAs("level '%s' lowest credits", level.name()).isNotNegative();
        }
    }
}
