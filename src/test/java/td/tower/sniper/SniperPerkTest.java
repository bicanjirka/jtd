package td.tower.sniper;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.FakeEnemyMob;
import td.tower.targeting.Viewpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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
    void steadyAimAddsTenPercentCritChanceForEveryStack() {
        SniperShot shot = new SteadyAimPerk().shape(BASE, context(3, false, 4));

        assertThat(shot.attack().critChance()).isCloseTo(0.35f, within(1e-6f));
    }

    @Test
    void steadyAimDoesNothingOnTheFirstShotAtATarget() {
        assertThat(new SteadyAimPerk().shape(BASE, context(0, true, 1))).isEqualTo(BASE);
    }

    @Test
    void quickScopeOnlyHelpsTheFirstShotAtANewTarget() {
        assertThat(new QuickScopePerk().shape(BASE, context(0, true, 1)).attack().critChance())
                .isCloseTo(0.55f, within(1e-6f));
        assertThat(new QuickScopePerk().shape(BASE, context(1, false, 2))).isEqualTo(BASE);
    }

    @Test
    void steadyTempoSpeedsUpEveryShotAfterTheFirstAtOneTarget() {
        assertThat(new SteadyTempoPerk().shape(BASE, context(1, false, 2)).fireRateBonus()).isEqualTo(0.25f);
        assertThat(new SteadyTempoPerk().shape(BASE, context(0, true, 1)).fireRateBonus()).isZero();
    }

    @Test
    void frenzyStartsOnACritAndNotOtherwise() {
        FrenzyPerk perk = new FrenzyPerk();
        EnemyMob target = FakeEnemyMob.at(0, 0);

        perk.react(new ShotResult(target, false, false), this.actions);
        perk.react(new ShotResult(target, true, false), this.actions);

        assertThat(this.actions.frenzies).isEqualTo(1);
    }

    @Test
    void weakSpotIgnoresPlatingAndSomeArmorOnlyOnShotsThatDoNotCrit() {
        AttackProfile attack = new WeakSpotPerk().shape(BASE, context(0, true, 1)).attack();

        assertThat(attack.penetrate(td.damage.DamageType.PHYSICAL, 80f, false)).isEqualTo(30f);
        assertThat(attack.penetrate(td.damage.DamageType.PHYSICAL, 80f, true)).isEqualTo(80f);
        assertThat(attack.penetratePlating(300f, false)).isZero();
        assertThat(attack.penetratePlating(300f, true)).isEqualTo(300f);
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
    void executionerMakesAWoundedBossTakeFiftyPercentMore() {
        EnemyMob woundedBoss = FakeEnemyMob.at(0, 0).ranked(Rank.BOSS).atHealthFraction(0.2f);
        EnemyMob healthyBoss = FakeEnemyMob.at(0, 0).ranked(Rank.BOSS).atHealthFraction(0.5f);

        assertThat(new ExecutionerPerk().shape(BASE, against(woundedBoss)).damageFactor()).isEqualTo(1.5f);
        assertThat(new ExecutionerPerk().shape(BASE, against(healthyBoss)).damageFactor()).isEqualTo(1f);
    }

    @Test
    void fifthShotGuaranteesEveryFifthCritAndOnlyThatOneHitsHarder() {
        FifthShotPerk perk = new FifthShotPerk();

        SniperShot fourth = perk.shape(BASE, context(0, true, 4));
        SniperShot fifth = perk.shape(BASE, context(0, true, 5));

        assertThat(fourth.attack().guaranteedCrit()).isFalse();
        assertThat(fifth.attack().guaranteedCrit()).isTrue();
        assertThat(fourth.attack().critMultiplier()).isEqualTo(2f);
        assertThat(fifth.attack().critMultiplier()).isEqualTo(2.5f);
    }

    @Test
    void momentumChargesAfterACritThenHitsFivefoldIgnoringArmorAndPlating() {
        MomentumPerk perk = new MomentumPerk();
        EnemyMob target = FakeEnemyMob.at(0, 0);
        assertThat(perk.shape(BASE, context(0, true, 1))).isEqualTo(BASE);

        perk.react(new ShotResult(target, true, false), this.actions);
        SniperShot charged = perk.shape(BASE, context(0, true, 2));
        perk.react(new ShotResult(target, false, false), this.actions);

        assertThat(charged.damageFactor()).isEqualTo(5f);
        assertThat(charged.attack().armorPenetration()).isEqualTo(1f);
        assertThat(charged.attack().platingPenetration()).isEqualTo(1f);
        assertThat(perk.shape(BASE, context(0, true, 3))).isEqualTo(BASE);
    }

    @Test
    void momentumStartsABurstOnAKill() {
        new MomentumPerk().react(new ShotResult(FakeEnemyMob.at(0, 0), false, true), this.actions);

        assertThat(this.actions.bursts).isEqualTo(1);
    }

    @Test
    void hollowPointMakesAnEnemyVulnerableOnlyOnACrit() {
        HollowPointPerk perk = new HollowPointPerk();
        EnemyMob target = FakeEnemyMob.at(0, 0);

        perk.react(new ShotResult(target, false, false), this.actions);
        perk.react(new ShotResult(target, true, false), this.actions);

        assertThat(this.actions.stackedWith(EffectKind.VULNERABLE)).containsExactly(target);
    }

    @Test
    void steadyAimStacksAndUnbrokenAimRaiseTheCapAndOnlyUnbrokenAimSurvivesAKill() {
        assertThat(new SteadyAimStacksPerk(3).refineSpec(SPEC).steadyAim()).isEqualTo(new AimRules(3, false));
        assertThat(new UnbrokenAimPerk().refineSpec(SPEC).steadyAim()).isEqualTo(new AimRules(5, true));
    }

    @Test
    void armorPierceAddsToWhatTheShotAlreadyIgnores() {
        SniperShot shot = BASE.withAttack(attack -> attack.withArmorPenetration(0.2f, 10f));

        AttackProfile attack = new ArmorPiercePerk(30f).shape(shot, context(0, true, 1)).attack();

        assertThat(attack.armorPenetration()).isEqualTo(0.2f);
        assertThat(attack.armorPenetrationFlat()).isEqualTo(40f);
    }

    @Test
    void cleanShotMakesCritsPierceShields() {
        assertThat(new CleanShotPerk().shape(BASE, context(0, true, 1)).attack().critsPierceShields()).isTrue();
    }

    @Test
    void sunderRoundsSundersOnlyOnACrit() {
        SunderRoundsPerk perk = new SunderRoundsPerk();
        EnemyMob target = FakeEnemyMob.at(0, 0);

        perk.react(new ShotResult(target, false, false), this.actions);
        perk.react(new ShotResult(target, true, false), this.actions);

        assertThat(this.actions.stackedWith(EffectKind.SUNDERED)).containsExactly(target);
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
    void longShotBoostsOnlyTargetsPastTwoThirdsOfTheRange() {
        LongShotPerk perk = new LongShotPerk();
        ShotContext far = new ShotContext(FakeEnemyMob.at(0, 0), new AimLock(0, true), 1, 0.7f);
        ShotContext near = new ShotContext(FakeEnemyMob.at(0, 0), new AimLock(0, true), 1, 0.6f);

        assertThat(perk.shape(BASE, far).damageFactor()).isEqualTo(1.25f);
        assertThat(perk.shape(BASE, near).damageFactor()).isEqualTo(1f);
    }

    @Test
    void silverRoundsMakeEveryThirdShotMagicWithMagicPenetration() {
        SilverRoundsPerk perk = new SilverRoundsPerk();

        SniperShot second = perk.shape(BASE, context(0, true, 2));
        SniperShot third = perk.shape(BASE, context(0, true, 3));

        assertThat(second.type()).isEqualTo(td.damage.DamageType.PHYSICAL);
        assertThat(third.type()).isEqualTo(td.damage.DamageType.MAGIC);
        assertThat(third.attack().magicPenetration()).isEqualTo(0.25f);
    }

    @Test
    void headhunterBoostsOnlyEliteAndBossTargets() {
        HeadhunterPerk perk = new HeadhunterPerk();

        assertThat(perk.shape(BASE, against(FakeEnemyMob.at(0, 0).ranked(Rank.VETERAN))).damageFactor()).isEqualTo(1f);
        assertThat(perk.shape(BASE, against(FakeEnemyMob.at(0, 0).ranked(Rank.ELITE))).damageFactor()).isEqualTo(1.4f);
        assertThat(perk.shape(BASE, against(FakeEnemyMob.at(0, 0).ranked(Rank.BOSS))).damageFactor()).isEqualTo(1.4f);
    }

    @Test
    void shatterShotMakesCritsOnAFrozenEnemyHitHarder() {
        FakeEnemyMob frozen = FakeEnemyMob.at(0, 0);
        frozen.reportFrozen();

        assertThat(new ShatterShotPerk().shape(BASE, against(frozen)).attack().critMultiplier()).isEqualTo(2.5f);
        assertThat(new ShatterShotPerk().shape(BASE, against(FakeEnemyMob.at(0, 0))).attack().critMultiplier())
                .isEqualTo(2f);
    }

    @Test
    void ricochetBouncesOnlyOnACrit() {
        RicochetPerk perk = new RicochetPerk();
        EnemyMob target = FakeEnemyMob.at(0, 0);

        perk.react(new ShotResult(target, false, false), this.actions);
        perk.react(new ShotResult(target, true, false), this.actions);

        assertThat(this.actions.ricochets).containsExactly(target);
    }

    @Test
    void eachSpecialSaysWhoItAimsAtAndTheRestSayNothing() {
        assertThat(new MomentumPerk().refineSpec(SPEC).aim()).map(SniperAim::label).contains("highest rank");
        assertThat(new HeadhunterPerk().refineSpec(SPEC).aim()).map(SniperAim::label).contains("highest rank");
        assertThat(new RicochetPerk().refineSpec(SPEC).aim()).map(SniperAim::label).contains("most neighbours");
        assertThat(new HollowPointPerk().refineSpec(SPEC).aim()).map(SniperAim::label).contains("most health");
        assertThat(new FifthShotPerk().refineSpec(SPEC).aim()).map(SniperAim::label).contains("most health");
        assertThat(new ShatterShotPerk().refineSpec(SPEC).aim()).map(SniperAim::label).contains("frozen or dazed first");
        assertThat(new SteadyAimPerk().refineSpec(SPEC).aim()).isEmpty();
    }
}
