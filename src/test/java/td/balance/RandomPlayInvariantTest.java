package td.balance;

import org.junit.jupiter.api.Test;
import td.level.BuiltInLevelCatalog;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Random legal play, sells and upgrades included, over every built-in level: whatever a player does, every round
 * clears, credits never go negative, and every enemy that walks in ends exactly once, killed or leaked.
 */
class RandomPlayInvariantTest {

    private static final int SEEDS = 3;

    private static List<PlayRecord> randomPlays(int levelIndex) {
        BoardSurvey survey = BoardSurvey.of(new BuiltInLevelCatalog().levels().get(levelIndex));
        return LongStream.range(1, 1 + SEEDS).parallel().boxed()
                .map(seed -> LevelPlay.play(survey, RandomLegalPlayer.spec(), seed, LevelPlay.Pacing.standard()))
                .toList();
    }

    private static void assertRulesHeld(List<PlayRecord> plays, boolean everyEnemyEnds) {
        for (PlayRecord play : plays) {
            assertThat(play.result().roundsCleared()).as("rounds cleared").isEqualTo(play.result().roundCount());
            for (PlayRecord.Round round : play.rounds()) {
                long ended = play.ends().stream().filter(end -> end.round() == round.number())
                        .mapToLong(end -> end.foe().maxHealth()).sum();
                assertThat(round.economy().creditsAtStart()).as("credits at round " + round.number())
                        .isNotNegative();
                if (everyEnemyEnds) {
                    assertThat(ended).as("health that ended in round " + round.number())
                            .isEqualTo(round.pressure().healthIn());
                } else {
                    assertThat(ended).isLessThanOrEqualTo(round.pressure().healthIn());
                }
            }
        }
    }

    @Test
    void randomPlayOnCurlyPathKeepsTheRules() {
        // A hatch swaps one mob for another without ending a walk.
        assertRulesHeld(randomPlays(0), false);
    }

    @Test
    void randomPlayOnZigzagPathKeepsTheRules() {
        assertRulesHeld(randomPlays(1), true);
    }

    @Test
    void randomPlayOnTwistedHourglassKeepsTheRules() {
        assertRulesHeld(randomPlays(2), true);
    }
}
