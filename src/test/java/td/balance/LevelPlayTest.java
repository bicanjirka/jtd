package td.balance;

import org.junit.jupiter.api.Test;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LevelPlayTest {

    private static final long SEED = 7L;

    private static BoardSurvey zigzag() {
        LevelDefinition level = new BuiltInLevelCatalog().levels().get(1);
        return BoardSurvey.of(level);
    }

    @Test
    void aSeededPlayRepeatsExactly() {
        BoardSurvey survey = zigzag();

        PlayRecord first = LevelPlay.play(survey, NaivePlayer.spec(), SEED, LevelPlay.Pacing.standard());
        PlayRecord second = LevelPlay.play(survey, NaivePlayer.spec(), SEED, LevelPlay.Pacing.standard());

        assertThat(second).isEqualTo(first);
    }

    @Test
    void aPlayedBackScriptEndsTheSameAsThePlayItCameFrom() {
        BoardSurvey survey = zigzag();
        PlayRecord played = LevelPlay.play(survey, NaivePlayer.spec(), SEED, LevelPlay.Pacing.standard());
        List<PlayRecord.TimedAction> script = played.actions().stream()
                .map(action -> PlayRecord.TimedAction.parse(action.toLine())).toList();

        PlayRecord replayed = LevelPlay.play(survey, ScriptedPlayer.spec("naive", script), SEED,
                LevelPlay.Pacing.standard());

        assertThat(replayed).isEqualTo(played);
    }

    @Test
    void aPlayThatLeaksPastItsLivesIsLostButCountsEveryLeak() {
        BoardSurvey survey = zigzag();

        PlayRecord idle = LevelPlay.play(survey, ScriptedPlayer.spec("idle", List.of()), SEED,
                LevelPlay.Pacing.standard());

        assertThat(idle.result().won()).isFalse();
        assertThat(idle.result().livesLost()).isEqualTo(survey.level().startingLives());
        assertThat(idle.result().leaks()).isEqualTo(idle.ends().size());
        assertThat(idle.rounds()).hasSize(idle.result().roundCount());
    }
}
