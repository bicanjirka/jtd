package td.tower.sniper;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.FakeEnemyMob;

import static org.assertj.core.api.Assertions.assertThat;

class SniperMechanicTest {

    private static final SniperShot BASE = SniperShot.of(AttackProfile.critChance(0.05f).withCritMultiplier(2f));

    private final RecordingShotActions actions = new RecordingShotActions();

    @Test
    void executionerKillsAWoundedEnemyBelowEliteThatACritLeftAndKeepsTheCrit() {
        EnemyMob wounded = FakeEnemyMob.at(0, 0).ranked(Rank.VETERAN).atHealthFraction(0.14f);

        ShotResult settled = new ExecutionerMechanic().settle(new ShotResult(wounded, true, false), this.actions);

        assertThat(this.actions.executed).containsExactly(wounded);
        assertThat(settled).isEqualTo(new ShotResult(wounded, true, true));
    }

    @Test
    void executionerLeavesAShotThatDidNotCritAHealthierEnemyAnEliteABossAndADeadOneAlone() {
        ExecutionerMechanic mechanic = new ExecutionerMechanic();
        ShotResult plain = new ShotResult(FakeEnemyMob.at(0, 0).atHealthFraction(0.05f), false, false);
        ShotResult healthy = new ShotResult(FakeEnemyMob.at(0, 0).atHealthFraction(0.15f), true, false);
        ShotResult elite = new ShotResult(FakeEnemyMob.at(0, 0).ranked(Rank.ELITE).atHealthFraction(0.05f), true, false);
        ShotResult boss = new ShotResult(FakeEnemyMob.at(0, 0).ranked(Rank.BOSS).atHealthFraction(0.05f), true, false);
        ShotResult dead = new ShotResult(FakeEnemyMob.at(0, 0).atHealthFraction(0f), true, true);

        assertThat(mechanic.settle(plain, this.actions)).isEqualTo(plain);
        assertThat(mechanic.settle(healthy, this.actions)).isEqualTo(healthy);
        assertThat(mechanic.settle(elite, this.actions)).isEqualTo(elite);
        assertThat(mechanic.settle(boss, this.actions)).isEqualTo(boss);
        assertThat(mechanic.settle(dead, this.actions)).isEqualTo(dead);
        assertThat(this.actions.executed).isEmpty();
    }

    @Test
    void anExecutionThatFailsToKillLeavesTheEnemyAlive() {
        this.actions.executionKills = false;
        EnemyMob wounded = FakeEnemyMob.at(0, 0).atHealthFraction(0.1f);

        ShotResult settled = new ExecutionerMechanic().settle(new ShotResult(wounded, true, false), this.actions);

        assertThat(settled).isEqualTo(new ShotResult(wounded, true, false));
    }

    @Test
    void ricochetHopsOnACritButNotOnAPlainHitOrAKill() {
        RicochetMechanic mechanic = new RicochetMechanic();
        EnemyMob plain = FakeEnemyMob.at(0, 0);
        EnemyMob crit = FakeEnemyMob.at(10, 0);
        EnemyMob killed = FakeEnemyMob.at(20, 0);

        mechanic.react(new ShotResult(plain, false, false), this.actions);
        mechanic.react(new ShotResult(crit, true, false), this.actions);
        mechanic.react(new ShotResult(killed, false, true), this.actions);

        assertThat(this.actions.ricochets).containsExactly(crit);
    }

    @Test
    void headhunterAddsDamageAgainstAnEliteAndABossButNotARankBelow() {
        HeadhunterMechanic mechanic = new HeadhunterMechanic();

        float veteran = mechanic.shape(BASE, new ShotContext(FakeEnemyMob.at(0, 0).ranked(Rank.VETERAN))).bonusDamage();
        float elite = mechanic.shape(BASE, new ShotContext(FakeEnemyMob.at(0, 0).ranked(Rank.ELITE))).bonusDamage();
        float boss = mechanic.shape(BASE, new ShotContext(FakeEnemyMob.at(0, 0).ranked(Rank.BOSS))).bonusDamage();

        assertThat(veteran).isZero();
        assertThat(elite).isPositive();
        assertThat(boss).isEqualTo(elite);
    }
}
