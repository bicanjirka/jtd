package td.balance;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;
import td.enemy.Rank;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;
import td.save.GameSave;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

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
    void aPlayLoadedFromASaveBeforeARoundPlaysOnlyThatRoundAndTheRestWithTheSavedTowers() {
        BoardSurvey survey = zigzag();
        PlayRecord played = LevelPlay.play(survey, NaivePlayer.spec(), SEED, LevelPlay.Pacing.standard());
        GameSave save = LevelPlay.saveBefore(survey, played.actions(), SEED, 5);

        PlayRecord fromSave = LevelPlay.play(survey, ScriptedPlayer.spec("idle", List.of()), SEED,
                LevelPlay.Pacing.standard(), Rules.standard(), Optional.of(save));

        assertThat(save.towers()).isNotEmpty();
        assertThat(fromSave.rounds()).extracting(PlayRecord.Round::number)
                .containsExactlyElementsOf(IntStream.rangeClosed(5, played.rounds().size()).boxed().toList());
        assertThat(fromSave.towers()).hasSameSizeAs(save.towers());
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

    @Test
    void aPlayCountsTheEffectsItsTowersPutOnEnemiesAndTheAbilitiesTheEnemiesUse() {
        BoardSurvey survey = zigzag();

        PlayRecord idle = LevelPlay.play(survey, ScriptedPlayer.spec("idle", List.of()), SEED,
                LevelPlay.Pacing.standard());
        PlayRecord played = LevelPlay.play(survey, NaivePlayer.spec(), SEED, LevelPlay.Pacing.standard());

        assertThat(played.actions()).anySatisfy(action -> assertThat(action.toLine()).contains("CINDER"));
        assertThat(idle.watch().effects().starts()).doesNotContainKey(EffectKind.BURN);
        assertThat(played.watch().effects().starts()).containsKey(EffectKind.BURN);
        assertThat(played.watch().effects().uptime(EffectKind.BURN)).isStrictlyBetween(0.0, 1.0);
        assertThat(played.watch().casts())
                .containsKey(PlayRecord.Watch.castKey(TowerLab.kindKey("g", Rank.VETERAN), EffectKind.INVISIBLE));
    }

    @Test
    void livesAddedByTheRulesAreLivesTheLevelHasToLose() {
        BoardSurvey survey = zigzag();

        PlayRecord idle = LevelPlay.play(survey, ScriptedPlayer.spec("idle", List.of()), SEED,
                LevelPlay.Pacing.standard(), Rules.standard().withLivesBonus(3));

        assertThat(idle.result().lives()).isEqualTo(survey.level().startingLives() + 3);
        assertThat(idle.result().livesLost()).isEqualTo(survey.level().startingLives() + 3);
    }
}
