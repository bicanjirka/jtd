package td.tower.sniper;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.FakeEnemyMob;
import td.tower.targeting.Viewpoint;

import static org.assertj.core.api.Assertions.assertThat;

class SniperPerkTest {

    private static final SniperShot BASE = SniperShot.of(AttackProfile.critChance(0.05f).withCritMultiplier(2f));

    private static final SniperSpec SPEC = SniperSpec.from(new Viewpoint(0, 0, 100f, 32));

    private final RecordingShotActions actions = new RecordingShotActions();

    private static ShotContext context(int stacks, boolean fresh, int shotNumber) {
        return new ShotContext(FakeEnemyMob.at(0, 0), new AimLock(stacks, fresh), shotNumber);
    }

    private static ShotContext against(EnemyMob target) {
        return new ShotContext(target, new AimLock(0, true), 1);
    }

    @Test
    void weakSpotIgnoresPlatingAndSomeArmorOnlyOnShotsThatDoNotCrit() {
        AttackProfile attack = new WeakSpotPerk().shape(BASE, context(0, true, 1)).attack();

        assertThat(attack.penetrate(td.damage.DamageType.PHYSICAL, 80f, false)).isEqualTo(30f);
        assertThat(attack.penetrate(td.damage.DamageType.PHYSICAL, 80f, true)).isEqualTo(80f);
        assertThat(attack.penetrateFlatReduction(300f, false)).isZero();
        assertThat(attack.penetrateFlatReduction(300f, true)).isEqualTo(300f);
    }

    @Test
    void railgunMakesTheShotPierce() {
        assertThat(new RailgunPerk().shape(BASE, context(0, true, 1)).piercing()).isTrue();
    }

    @Test
    void executionerKillsAWoundedNonBossAndCountsItAsACrit() {
        EnemyMob wounded = FakeEnemyMob.at(0, 0).ranked(Rank.ELITE).atHealthFraction(0.14f);

        ShotResult settled = new ExecutionerPerk().settle(new ShotResult(wounded, false, false), this.actions);

        assertThat(this.actions.executed).containsExactly(wounded);
        assertThat(settled).isEqualTo(new ShotResult(wounded, true, true));
    }

    @Test
    void executionerLeavesAHealthierEnemyABossAndAnAlreadyDeadOneAlone() {
        ExecutionerPerk perk = new ExecutionerPerk();
        ShotResult healthy = new ShotResult(FakeEnemyMob.at(0, 0).atHealthFraction(0.15f), false, false);
        ShotResult boss = new ShotResult(FakeEnemyMob.at(0, 0).ranked(Rank.BOSS).atHealthFraction(0.05f), false, false);
        ShotResult dead = new ShotResult(FakeEnemyMob.at(0, 0).atHealthFraction(0f), true, true);

        assertThat(perk.settle(healthy, this.actions)).isEqualTo(healthy);
        assertThat(perk.settle(boss, this.actions)).isEqualTo(boss);
        assertThat(perk.settle(dead, this.actions)).isEqualTo(dead);
        assertThat(this.actions.executed).isEmpty();
    }

    @Test
    void anExecutionThatFailsToKillStillCountsAsACrit() {
        this.actions.executionKills = false;
        EnemyMob wounded = FakeEnemyMob.at(0, 0).atHealthFraction(0.1f);

        ShotResult settled = new ExecutionerPerk().settle(new ShotResult(wounded, false, false), this.actions);

        assertThat(settled).isEqualTo(new ShotResult(wounded, true, false));
    }

    @Test
    void onlyUnbrokenAimLetsSteadyAimSurviveAKill() {
        assertThat(SPEC.steadyAim()).isEqualTo(new AimRules(false));
        assertThat(new UnbrokenAimPerk().refineSpec(SPEC).steadyAim()).isEqualTo(new AimRules(true));
    }

    @Test
    void critStreakMakesTheNextCritHitHarderAndTwoInARowHarderStillButNoFurther() {
        CritStreakPerk perk = new CritStreakPerk();
        EnemyMob target = FakeEnemyMob.at(0, 0);
        ShotResult crit = new ShotResult(target, true, false);

        float none = perk.shape(BASE, context(0, true, 1)).attack().critMultiplier();
        perk.react(crit, this.actions);
        float one = perk.shape(BASE, context(0, true, 2)).attack().critMultiplier();
        perk.react(crit, this.actions);
        float two = perk.shape(BASE, context(0, true, 3)).attack().critMultiplier();
        perk.react(crit, this.actions);
        float capped = perk.shape(BASE, context(0, true, 4)).attack().critMultiplier();

        assertThat(none).isEqualTo(2f);
        assertThat(one).isEqualTo(2.25f);
        assertThat(two).isEqualTo(2.5f);
        assertThat(capped).isEqualTo(2.5f);
    }

    @Test
    void aShotThatDoesNotCritEndsTheCritStreak() {
        CritStreakPerk perk = new CritStreakPerk();
        EnemyMob target = FakeEnemyMob.at(0, 0);
        perk.react(new ShotResult(target, true, false), this.actions);

        perk.react(new ShotResult(target, false, false), this.actions);

        assertThat(perk.shape(BASE, context(0, true, 3)).attack().critMultiplier()).isEqualTo(2f);
    }

    @Test
    void ricochetBouncesOnlyOnACrit() {
        RicochetPerk perk = new RicochetPerk();
        EnemyMob target = FakeEnemyMob.at(0, 0);

        perk.react(new ShotResult(target, false, false), this.actions);
        perk.react(new ShotResult(target, true, false), this.actions);

        assertThat(this.actions.ricochets).containsExactly(target);
    }

}
