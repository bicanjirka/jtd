package td.balance;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PlayCheckTest {

    /** A play of a level of {@code lives} lives whose rounds leaked {@code leaks} and spent {@code upgrades} of 100 on nodes. */
    private static PlayRecord play(int lives, int upgrades, int... leaks) {
        List<PlayRecord.Round> rounds = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < leaks.length; i++) {
            total += leaks[i];
            rounds.add(new PlayRecord.Round(i + 1, 100,
                    new PlayRecord.RoundEconomy(0, new PlayRecord.Income(0, 0),
                            new PlayRecord.Spending(100 - upgrades, upgrades, 0), leaks[i]),
                    new PlayRecord.RoundPressure(100, leaks[i], 1.0, Map.of())));
        }
        return new PlayRecord(new PlayRecord.Play("level", 1L, "player"),
                new PlayRecord.Result(lives, total, leaks.length, leaks.length), rounds,
                new PlayRecord.Log(List.of(), List.of(), List.of(), new PlayRecord.Watch(
                        new PlayRecord.EffectUse(0, Map.of(), Map.of()), Map.of(), List.of(), Map.of())));
    }

    @Test
    void aPlayerThatShouldClearFailsOnASingleLeakAndSaysWhere() {
        assertThat(PlayCheck.verdict(PlayCheck.Expect.CLEARS, play(3, 0, 0, 0, 0))).startsWith("ok");
        assertThat(PlayCheck.verdict(PlayCheck.Expect.CLEARS, play(3, 0, 0, 1, 0))).startsWith("FAIL").endsWith("round 2");
    }

    @Test
    void aPlayerThatShouldLoseIsRightOnlyOnceTheLivesAreGone() {
        assertThat(PlayCheck.verdict(PlayCheck.Expect.LOSES, play(3, 0, 1, 1, 1))).isEqualTo("ok: loses in round 3");
        assertThat(PlayCheck.verdict(PlayCheck.Expect.LOSES, play(3, 0, 0, 2, 0))).startsWith("WARN");
        assertThat(PlayCheck.verdict(PlayCheck.Expect.LOSES, play(3, 0, 0, 0, 0))).startsWith("BREACH");
    }

    @Test
    void aPlayThatBuysNoUpgradeIsWarnedOfAndOneThatBuysManyIsNot() {
        assertThat(PlayCheck.upgradeVerdict(play(3, 0, 0, 0, 0))).startsWith("WARN: no upgrade");
        assertThat(PlayCheck.upgradeVerdict(play(3, 60, 0, 0, 0))).startsWith("ok: upgrades take 60%");
    }

    @Test
    void theBestEffortMayBuyEverythingJudgedAndEveryOtherPlayerLessAndMustLose() {
        List<PlayCheck.Contestant> contestants = PlayCheck.contestants(0);

        PlayCheck.Contestant best = contestants.getFirst();
        assertThat(best.expect()).isEqualTo(PlayCheck.Expect.CLEARS);
        assertThat(contestants.stream().skip(1)).allSatisfy(other -> {
            assertThat(other.expect()).isEqualTo(PlayCheck.Expect.LOSES);
            assertThat(best.limits().types()).containsAll(other.limits().types());
            assertThat(other.limits().upgrades() && other.limits().types().equals(best.limits().types())).isFalse();
        });
    }
}
