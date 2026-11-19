package td.tower.splash;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.fixtures.FakeEnemyMob;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HexLedgerTest {

    private static FakeEnemyMob doomed() {
        FakeEnemyMob enemy = FakeEnemyMob.at(0, 0);
        enemy.applyEffect(Effect.hex(EffectKind.DOOM, 80, DamageSink.none()));
        return enemy;
    }

    @Test
    void aHexStillRunningIsWatchedQuietly() {
        HexLedger ledger = new HexLedger();
        ledger.record(doomed(), EffectKind.DOOM, 80);

        assertThat(ledger.settle()).isEmpty();
        assertThat(ledger.isEmpty()).isFalse();
    }

    @Test
    void aHexThatRanOutReportsTheDamageTakenSinceItWasCast() {
        HexLedger ledger = new HexLedger();
        FakeEnemyMob enemy = doomed();
        enemy.doDamage(Damage.physical(500), AttackProfile.none());
        ledger.record(enemy, EffectKind.DOOM, 80);
        enemy.doDamage(Damage.physical(300), AttackProfile.none());
        enemy.doDamage(Damage.magic(200), AttackProfile.none());

        enemy.expire(EffectKind.DOOM);

        assertThat(ledger.settle()).containsExactly(new HexEvent.Ended(enemy, EffectKind.DOOM, 500));
        assertThat(ledger.isEmpty()).isTrue();
    }

    @Test
    void anEnemyThatIsGoneIsReportedOnceWithEveryHexItCarriedAndHowLongEachWasCastFor() {
        HexLedger ledger = new HexLedger();
        FakeEnemyMob enemy = doomed();
        enemy.dieOnAnyHit();
        ledger.record(enemy, EffectKind.DOOM, 80);
        ledger.record(enemy, EffectKind.BLIGHT, 40);
        enemy.doDamage(Damage.physical(1), AttackProfile.none());

        assertThat(ledger.settle()).containsExactly(new HexEvent.Gone(enemy, List.of(
                new HexEvent.CarriedHex(EffectKind.DOOM, 80, 1), new HexEvent.CarriedHex(EffectKind.BLIGHT, 40, 1))));
        assertThat(ledger.isEmpty()).isTrue();
    }

    @Test
    void recastingAHexOnAnEnemyStartsItsCountAgainFromNow() {
        HexLedger ledger = new HexLedger();
        FakeEnemyMob enemy = doomed();
        ledger.record(enemy, EffectKind.DOOM, 80);
        enemy.doDamage(Damage.physical(400), AttackProfile.none());
        ledger.record(enemy, EffectKind.DOOM, 80);
        enemy.doDamage(Damage.physical(100), AttackProfile.none());

        enemy.expire(EffectKind.DOOM);

        assertThat(ledger.settle()).containsExactly(new HexEvent.Ended(enemy, EffectKind.DOOM, 100));
    }

    @Test
    void aHexThatJumpedInReportsWhatItCarriedPlusWhatTheEnemyTookSince() {
        HexLedger ledger = new HexLedger();
        FakeEnemyMob enemy = doomed();
        enemy.doDamage(Damage.physical(900), AttackProfile.none());
        ledger.recordJump(enemy, EffectKind.DOOM, 80, 250);
        enemy.doDamage(Damage.physical(100), AttackProfile.none());

        enemy.expire(EffectKind.DOOM);

        assertThat(ledger.settle()).containsExactly(new HexEvent.Ended(enemy, EffectKind.DOOM, 350));
    }

    @Test
    void aHexThatJumpedOntoOneAlreadyThereMergesBothRecordsAndKeepsTheLongerCast() {
        HexLedger ledger = new HexLedger();
        FakeEnemyMob enemy = doomed();
        ledger.record(enemy, EffectKind.DOOM, 100);
        enemy.doDamage(Damage.physical(400), AttackProfile.none());
        ledger.recordJump(enemy, EffectKind.DOOM, 60, 250);
        enemy.doDamage(Damage.physical(100), AttackProfile.none());

        enemy.expire(EffectKind.DOOM);

        assertThat(ledger.settle()).containsExactly(new HexEvent.Ended(enemy, EffectKind.DOOM, 750));
    }
}
