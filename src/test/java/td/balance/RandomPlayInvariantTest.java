package td.balance;

import org.junit.jupiter.api.Test;
import td.enemy.Ability;
import td.enemy.ApplyEffectAction;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.LeaveZoneAction;
import td.enemy.Rank;
import td.enemy.SpawnEnemiesAction;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;
import td.wave.PathDefinition;
import td.wave.WaveDefinition;
import td.wave.WaveScript;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Random legal play, sells and upgrades included, over every built-in level: whatever a player does, every round
 * clears, credits never go negative, and every enemy that walks in ends exactly once, killed or leaked.
 */
class RandomPlayInvariantTest {

    private static final int SEEDS = 3;

    private static List<PlayRecord> randomPlays(LevelDefinition level) {
        BoardSurvey survey = BoardSurvey.of(level);
        return LongStream.range(1, 1 + SEEDS).parallel().boxed()
                .map(seed -> LevelPlay.play(survey, RandomLegalPlayer.spec(), seed, LevelPlay.Pacing.standard()))
                .toList();
    }

    @Test
    void randomPlayOnEveryBuiltInLevelKeepsTheRules() {
        for (LevelDefinition level : new BuiltInLevelCatalog().levels()) {
            // A hatch swaps one mob for another without ending a walk, so only an end that never came is allowed.
            boolean everyEnemyEnds = !someEnemyHatches(level);

            for (PlayRecord play : randomPlays(level)) {
                assertThat(play.result().roundsCleared()).as("%s rounds cleared", level.name())
                        .isEqualTo(play.result().roundCount());
                for (PlayRecord.Round round : play.rounds()) {
                    long ended = play.ends().stream().filter(end -> end.round() == round.number())
                            .mapToLong(end -> end.foe().maxHealth()).sum();
                    assertThat(round.economy().creditsAtStart()).as("%s credits at round %d", level.name(),
                            round.number()).isNotNegative();
                    if (everyEnemyEnds) {
                        assertThat(ended).as("%s health that ended in round %d", level.name(), round.number())
                                .isEqualTo(round.pressure().healthIn());
                    } else {
                        assertThat(ended).isLessThanOrEqualTo(round.pressure().healthIn());
                    }
                }
            }
        }
    }

    /** Whether an enemy the level sends, or one spawned along the way, replaces itself with another. */
    private static boolean someEnemyHatches(LevelDefinition level) {
        EnemyCatalog catalog = EnemyCatalog.builtIn();
        level.customEnemies().forEach(catalog::register);
        level.customRankedEnemies().forEach(catalog::register);
        Deque<EnemyDefinition> open = new ArrayDeque<>();
        for (PathDefinition path : level.paths()) {
            for (WaveDefinition wave : path.waves()) {
                open.addAll(WaveScript.parse(wave.enemies(), wave.rank(), catalog).enemySet());
            }
        }
        Set<String> seen = new HashSet<>();
        while (!open.isEmpty()) {
            for (Ability ability : open.pop().abilities()) {
                switch (ability.action()) {
                    case SpawnEnemiesAction spawn -> {
                        if (spawn.consumesSelf()) {
                            return true;
                        }
                        if (seen.add(spawn.definitionId())) {
                            for (Rank rank : Rank.values()) {
                                open.add(catalog.get(spawn.definitionId(), rank));
                            }
                        }
                    }
                    case ApplyEffectAction ignored -> {
                    }
                    case LeaveZoneAction ignored -> {
                    }
                }
            }
        }
        return false;
    }
}
