package td.balance;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;

import java.util.List;
import java.util.stream.LongStream;

/**
 * Plays every built-in level with each kind of player over many seeds and reports how it went. A tool run on
 * demand, not a test.
 */
public final class BalanceReport {

    private static final long FIRST_SEED = 20261013L;

    private BalanceReport() {
    }

    public static void main(String[] args) {
        quietLogs();
        int seeds = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        java.util.Map<String, TowerLab> labs = new java.util.HashMap<>();
        List<PlayerSpec> players = List.of(NaivePlayer.spec(),
                CompetentPlayer.spec("competent", labs, CompetentPlayer.Style.standard()),
                CompetentPlayer.spec("random-cells", labs, CompetentPlayer.Style.standard().withRandomCells()),
                CompetentPlayer.spec("no-upgrades", labs, CompetentPlayer.Style.standard().withoutUpgrades()));
        for (LevelDefinition level : new BuiltInLevelCatalog().levels()) {
            BoardSurvey survey = BoardSurvey.of(level);
            labs.put(level.name(), TowerLab.of(survey));
            System.out.printf("=== %s: %d credits, %d lives, %d rounds ===%n", level.name(), level.startingCredits(),
                    level.startingLives(), level.paths().getFirst().waves().size());
            for (PlayerSpec player : players) {
                long started = System.nanoTime();
                List<PlayRecord> plays = LongStream.range(FIRST_SEED, FIRST_SEED + seeds).parallel().boxed()
                        .map(seed -> LevelPlay.play(survey, player, seed, LevelPlay.Pacing.standard())).toList();
                double seconds = (System.nanoTime() - started) / 1e9;
                Estimate won = Estimate.proportion(plays, play -> play.result().won());
                Estimate lives = Estimate.of(plays, play -> play.result().livesLost());
                Estimate leaks = Estimate.of(plays, play -> play.result().leaks());
                long marginStart = System.nanoTime();
                double[] margins = LongStream.range(FIRST_SEED, FIRST_SEED + seeds).parallel()
                        .mapToDouble(seed -> Margin.healthPercent(survey, player, seed, LevelPlay.Pacing.standard()))
                        .toArray();
                System.out.printf("  %-12s won %s  lives lost %s  leaks %s  holds %s%% health  (%.1fs + %.1fs)%n",
                        player.name(), won.formatPercent(), lives.format(1), leaks.format(1),
                        Estimate.of(margins).format(0), seconds, (System.nanoTime() - marginStart) / 1e9);
            }
        }
    }

    /** The engine logs every placement and round at INFO; thousands of plays would drown the report. */
    static void quietLogs() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.WARN);
    }
}
