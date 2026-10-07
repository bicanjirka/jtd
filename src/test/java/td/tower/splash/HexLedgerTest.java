package td.tower.splash;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectKind;
import td.fixtures.FakeEnemyMob;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HexLedgerTest {

    private static FakeEnemyMob doomed() {
        FakeEnemyMob enemy = FakeEnemyMob.at(0, 0);
        enemy.applyEffect(Effect.hex(EffectKind.DOOM, 80, d -> {
        }));
        return enemy;
    }

    @Test
    void aHexStillRunningIsWatchedQuietly() {
        HexLedger ledger = new HexLedger();
        ledger.record(doomed(), EffectKind.DOOM);

        assertThat(ledger.settle()).isEmpty();
        assertThat(ledger.isEmpty()).isFalse();
    }

    @Test
    void aHexThatRanOutReportsTheDamageTakenSinceItWasCast() {
        HexLedger ledger = new HexLedger();
        FakeEnemyMob enemy = doomed();
        enemy.doDamage(Damage.physical(500), AttackProfile.none());
        ledger.record(enemy, EffectKind.DOOM);
        enemy.doDamage(Damage.physical(300), AttackProfile.none());
        enemy.doDamage(Damage.magic(200), AttackProfile.none());

        enemy.expire(EffectKind.DOOM);

        assertThat(ledger.settle()).containsExactly(new HexEvent.Ended(enemy, EffectKind.DOOM, 500));
        assertThat(ledger.isEmpty()).isTrue();
    }

    @Test
    void anEnemyThatDiedIsReportedOnceWithEveryHexItCarriedAndTheirJumps() {
        HexLedger ledger = new HexLedger();
        FakeEnemyMob enemy = doomed();
        enemy.dieOnAnyHit();
        ledger.record(enemy, EffectKind.DOOM);
        ledger.record(enemy, EffectKind.CONTAGION, 1);
        enemy.doDamage(Damage.physical(1), AttackProfile.none());

        assertThat(ledger.settle()).containsExactly(new HexEvent.Died(enemy, List.of(
                new HexEvent.CarriedHex(EffectKind.DOOM, 0), new HexEvent.CarriedHex(EffectKind.CONTAGION, 1))));
        assertThat(ledger.isEmpty()).isTrue();
    }

    @Test
    void carriersAreTheLivingEnemiesWithThatHexAndHexedIsEveryLivingOneOnce() {
        HexLedger ledger = new HexLedger();
        FakeEnemyMob first = doomed();
        FakeEnemyMob second = doomed();
        FakeEnemyMob dead = doomed();
        ledger.record(first, EffectKind.DOOM);
        ledger.record(first, EffectKind.SYMPATHY);
        ledger.record(second, EffectKind.DOOM);
        ledger.record(dead, EffectKind.SYMPATHY);
        dead.invalidate();

        assertThat(ledger.carriers(EffectKind.SYMPATHY)).containsExactly(first);
        assertThat(ledger.carriers(EffectKind.DOOM)).containsExactly(first, second);
        assertThat(ledger.hexed()).containsExactly(first, second);
    }

    @Test
    void restartingAHexCountsOutWhatItStoredAndStartsTheCountAgainFromNow() {
        HexLedger ledger = new HexLedger();
        FakeEnemyMob enemy = doomed();
        ledger.record(enemy, EffectKind.DOOM);
        enemy.doDamage(Damage.physical(400), AttackProfile.none());

        long first = ledger.restart(enemy, EffectKind.DOOM);
        enemy.doDamage(Damage.physical(100), AttackProfile.none());
        long second = ledger.restart(enemy, EffectKind.DOOM);

        assertThat(first).isEqualTo(400);
        assertThat(second).isEqualTo(100);
        assertThat(ledger.restart(enemy, EffectKind.SYMPATHY)).isZero();
    }
}
