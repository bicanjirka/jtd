package td.balance;

import org.junit.jupiter.api.Test;
import td.effect.EffectKind;
import td.enemy.Rank;
import td.level.LevelDefinition;
import td.save.GameSave;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.smoothing.ArcCornerSmoothing;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class LevelPlayTest {

    private static final long SEED = 5L;

    private static final ArcCornerSmoothing SMOOTHING = new ArcCornerSmoothing(0.4, 10);

    /** Two crossing lanes of built-in enemies, Veteran Ghosts among them, on a board where the naive player builds. */
    private static BoardSurvey twoLanes() {
        LevelDefinition level = LevelDefinition.of("Two Lanes", 16, 11, List.of(
                PathDefinition.smoothed(List.of(new Point(-1, 2), new Point(13, 2), new Point(13, 5), new Point(2, 5),
                                new Point(2, 8), new Point(16, 8)),
                List.of(
                    new WaveDefinition("simple e simple e simple e simple e 4 simple e 3 armored", Rank.GRUNT),
                    new WaveDefinition("5 simple e 4 simple e 2 armored", Rank.GRUNT),
                    new WaveDefinition("armored e armored e armored e 4 simple e armored e armored", Rank.SOLDIER),
                    new WaveDefinition("fast e fast e fast e fast e 4 simple e 2 armored e fast", Rank.SOLDIER),
                    new WaveDefinition("3 armored 2 e 3 simple 2 e 4 armored 2 e 4 simple", Rank.SOLDIER),
                    new WaveDefinition("ghost e ghost e ghost e 3 armored e 3 simple e ghost", Rank.VETERAN),
                    new WaveDefinition("2 ghost 2 fast 2 armored 2 simple 2 e 2 ghost 2 fast 4 simple", Rank.VETERAN),
                    new WaveDefinition("10 simple e 5 armored e 3 fast e ghost e 8 simple e 4 armored e 2 ghost",
                                        Rank.ELITE)),
                SMOOTHING),
                PathDefinition.smoothed(List.of(new Point(12, -1), new Point(12, 2), new Point(9, 10), new Point(7, 10),
                                new Point(3, 2), new Point(3, -1)),
                List.of(
                    new WaveDefinition("simple e simple e simple e 4 simple e 3 armored", Rank.GRUNT),
                    new WaveDefinition("5 simple e 4 simple e 2 armored", Rank.GRUNT),
                    new WaveDefinition("armored e armored e armored e 4 simple e armored e armored", Rank.SOLDIER),
                    new WaveDefinition("fast e fast e fast e 4 simple e 2 armored e fast", Rank.SOLDIER),
                    new WaveDefinition("3 armored 2 e 3 simple 2 e 4 armored 2 e 4 simple", Rank.SOLDIER),
                    new WaveDefinition("ghost e ghost e ghost e 3 armored e 3 simple e ghost", Rank.VETERAN),
                    new WaveDefinition("2 ghost 2 fast 2 armored 2 e 2 ghost 2 fast 4 simple", Rank.VETERAN),
                    new WaveDefinition("10 simple e 5 armored e ghost e 8 simple e 4 armored e ghost", Rank.ELITE)),
                SMOOTHING)))
                .withStartingCredits(75)
                .withStartingLives(3);
        return BoardSurvey.of(level);
    }

    /** One round of one Grunt too frail to reach the end, paying {@code bonus} for a clean clear. */
    private static BoardSurvey oneCleanRound(int bonus) {
        return BoardSurvey.of(LevelDefinition.of("One Round", 16, 11, List.of(
                PathDefinition.smoothed(List.of(new Point(-1, 5), new Point(16, 5)),
                        List.of(new WaveDefinition("simple", Rank.GRUNT).withClearBonus(bonus)), SMOOTHING)))
                .withStartingCredits(75)
                .withStartingLives(3));
    }

    @Test
    void aCleanRoundRecordsItsClearBonusApartFromItsBounty() {
        Rules frail = Rules.standard().withEnemyHealthPercent(1).withClearBonusPercent(200);

        PlayRecord play = LevelPlay.play(oneCleanRound(5), NaivePlayer.spec(), SEED, LevelPlay.Pacing.standard(), frail);

        PlayRecord.Income income = play.rounds().getFirst().economy().income();
        assertThat(income.clearBonus()).isEqualTo(5 * 2);
        assertThat(income.total()).isEqualTo(income.bounty() + income.clearBonus());
    }

    @Test
    void aSeededPlayRepeatsExactly() {
        BoardSurvey survey = twoLanes();

        PlayRecord first = LevelPlay.play(survey, NaivePlayer.spec(), SEED, LevelPlay.Pacing.standard());
        PlayRecord second = LevelPlay.play(survey, NaivePlayer.spec(), SEED, LevelPlay.Pacing.standard());

        assertThat(second).isEqualTo(first);
    }

    @Test
    void aPlayedBackScriptEndsTheSameAsThePlayItCameFrom() {
        BoardSurvey survey = twoLanes();
        PlayRecord played = LevelPlay.play(survey, NaivePlayer.spec(), SEED, LevelPlay.Pacing.standard());
        List<PlayRecord.TimedAction> script = played.actions().stream()
                .map(action -> PlayRecord.TimedAction.parse(action.toLine())).toList();

        PlayRecord replayed = LevelPlay.play(survey, ScriptedPlayer.spec("naive", script), SEED,
                LevelPlay.Pacing.standard());

        assertThat(replayed).isEqualTo(played);
    }

    @Test
    void aPlayLoadedFromASaveBeforeARoundPlaysOnlyThatRoundAndTheRestWithTheSavedTowers() {
        BoardSurvey survey = twoLanes();
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
        BoardSurvey survey = twoLanes();

        PlayRecord idle = LevelPlay.play(survey, ScriptedPlayer.spec("idle", List.of()), SEED,
                LevelPlay.Pacing.standard());

        assertThat(idle.result().won()).isFalse();
        assertThat(idle.result().livesLost()).isEqualTo(survey.level().startingLives());
        assertThat(idle.result().leaks()).isEqualTo(idle.ends().size());
        assertThat(idle.rounds()).hasSize(idle.result().roundCount());
    }

    @Test
    void aPlaysTowerWorkAddsUpToTheDamageItsRoundsRecordedAndSplitsItByEnemyKind() {
        PlayRecord played = LevelPlay.play(twoLanes(), NaivePlayer.spec(), SEED, LevelPlay.Pacing.standard());

        long byRound = played.rounds().stream().flatMap(round -> round.pressure().damage().values().stream())
                .mapToLong(Long::longValue).sum();
        long credited = played.watch().towers().values().stream().mapToLong(work -> work.hits().credited()).sum();
        long byKind = played.watch().towers().values().stream()
                .flatMap(work -> work.damageByKind().values().stream()).mapToLong(Long::longValue).sum();

        assertThat(credited).isPositive().isEqualTo(byRound).isEqualTo(byKind);
        assertThat(played.watch().towers().values()).anySatisfy(work -> assertThat(work.ticksInReach()).isPositive());
        assertThat(played.watch().towers().values()).anySatisfy(work -> assertThat(work.hits().overkill()).isPositive());
    }

    @Test
    void aPlayCountsTheEffectsItsTowersPutOnEnemiesAndTheAbilitiesTheEnemiesUse() {
        BoardSurvey survey = twoLanes();

        PlayRecord idle = LevelPlay.play(survey, ScriptedPlayer.spec("idle", List.of()), SEED,
                LevelPlay.Pacing.standard());
        PlayRecord played = LevelPlay.play(survey, NaivePlayer.spec(), SEED, LevelPlay.Pacing.standard());

        assertThat(played.actions()).anySatisfy(action -> assertThat(action.toLine()).contains("CINDER"));
        assertThat(idle.watch().effects().starts()).doesNotContainKey(EffectKind.BURN);
        assertThat(played.watch().effects().starts()).containsKey(EffectKind.BURN);
        assertThat(played.watch().effects().uptime(EffectKind.BURN)).isStrictlyBetween(0.0, 1.0);
        assertThat(played.watch().casts())
                .containsKey(PlayRecord.Watch.castKey(TowerLab.kindKey("ghost", Rank.VETERAN), EffectKind.INVISIBLE));
    }

    @Test
    void livesAddedByTheRulesAreLivesTheLevelHasToLose() {
        BoardSurvey survey = twoLanes();

        PlayRecord idle = LevelPlay.play(survey, ScriptedPlayer.spec("idle", List.of()), SEED,
                LevelPlay.Pacing.standard(), Rules.standard().withLivesBonus(3));

        assertThat(idle.result().lives()).isEqualTo(survey.level().startingLives() + 3);
        assertThat(idle.result().livesLost()).isEqualTo(survey.level().startingLives() + 3);
    }
}
