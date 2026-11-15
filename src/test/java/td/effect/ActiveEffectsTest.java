package td.effect;

import org.junit.jupiter.api.Test;
import td.damage.AttackOrigin;
import td.damage.Damage;
import td.damage.DamageType;
import td.damage.DamageUnits;
import td.stat.BaseStats;
import td.stat.EnemyStat;
import td.stat.StatCurves;
import td.stat.StatSheet;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ActiveEffectsTest {

    private static float resolved(ActiveEffects effects, EnemyStat stat) {
        return new StatSheet(BaseStats.defaults(EnemyStat.class).with(EnemyStat.MOVE_SPEED, 1f), effects::contributeTo).value(stat);
    }

    private static DamageSink recordingSink(List<Damage> received) {
        return received::add;
    }

    @Test
    void aPoolReadsFullerTheLongerItWouldBurnAndFullOnceItWouldBurnLongEnough() {
        ActiveEffects small = new ActiveEffects();
        ActiveEffects large = new ActiveEffects();
        ActiveEffects huge = new ActiveEffects();

        small.apply(Effect.burn(Damage.magic(1_000), DamageSink.none()));
        large.apply(Effect.burn(Damage.magic(4_000), DamageSink.none()));
        huge.apply(Effect.burn(Damage.magic(10_000_000), DamageSink.none()));

        assertThat(large.fullness(EffectKind.BURN).orElseThrow())
                .isCloseTo(2 * small.fullness(EffectKind.BURN).orElseThrow(), within(1e-4));
        assertThat(huge.fullness(EffectKind.BURN).orElseThrow()).isEqualTo(1.0);
    }

    @Test
    void aRefreshedStackKeepsTheLongerClock() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.vulnerable(1, 100, DamageSink.none()));

        effects.apply(Effect.vulnerable(1, 40, DamageSink.none()));

        assertThat(effects.clockTicks(EffectKind.VULNERABLE).orElseThrow()).isGreaterThanOrEqualTo(100);
    }

    @Test
    void withNoActiveEffectsEveryStatKeepsItsBase() {
        BaseStats<EnemyStat> base = BaseStats.defaults(EnemyStat.class).with(EnemyStat.MOVE_SPEED, 1f);

        StatSheet sheet = new StatSheet(base, new ActiveEffects()::contributeTo);

        for (EnemyStat stat : EnemyStat.values()) {
            assertThat(sheet.value(stat)).as(stat.name()).isEqualTo(base.value(stat));
        }
    }

    @Test
    void aSlowHoldsItsStrengthThenFadesOverTheEndOfItsTime() {
        ActiveEffects effects = new ActiveEffects();
        int duration = 2 * ActiveEffects.SLOW_FADE_TICKS;
        effects.apply(Effect.chill(0.4f, duration, DamageSink.none()));

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.6f, within(0.001f));

        tickTimes(effects, ActiveEffects.SLOW_FADE_TICKS);
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.6f, within(0.001f));

        tickTimes(effects, ActiveEffects.SLOW_FADE_TICKS / 2);
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.8f, within(0.001f));
        assertThat(effects.activeKinds()).containsExactly(EffectKind.CHILL);

        tickTimes(effects, ActiveEffects.SLOW_FADE_TICKS - ActiveEffects.SLOW_FADE_TICKS / 2);
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isEqualTo(1f);
        assertThat(effects.activeKinds()).isEmpty();
    }

    @Test
    void aSlowShorterThanTheFadeFadesOverItsWholeLength() {
        ActiveEffects effects = new ActiveEffects();
        int duration = ActiveEffects.SLOW_FADE_TICKS / 2;
        effects.apply(Effect.chill(0.4f, duration, DamageSink.none()));

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.6f, within(0.001f));

        tickTimes(effects, 1);

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED))
                .isCloseTo(1f - 0.4f * (duration - 1) / duration, within(0.001f));
    }

    @Test
    void aGrowingSlowGrowsByTheTickItHasRunFromItsStart() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.blight(100, 0.01f, 0.05f, 0f, DamageSink.none()));

        for (int i = 0; i < 10; i++) {
            effects.tick();
        }

        assertThat(effects.slowNow(EffectKind.BLIGHT)).isCloseTo(0.05f + 10 * 0.01f, within(0.001f));
    }

    @Test
    void aGrowingSlowNeverTakesLessThanItsFloor() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.blight(100, 0.01f, 0f, 0.3f, DamageSink.none()));

        effects.tick();

        assertThat(effects.slowNow(EffectKind.BLIGHT)).isCloseTo(0.3f, within(0.001f));
    }

    @Test
    void aRefreshedSlowStaysFull() {
        ActiveEffects effects = new ActiveEffects();
        for (int i = 0; i < 3 * ActiveEffects.SLOW_FADE_TICKS; i++) {
            effects.apply(Effect.tarred(ActiveEffects.SLOW_FADE_TICKS, DamageSink.none()));

            assertThat(resolved(effects, EnemyStat.MOVE_SPEED))
                    .isCloseTo(1f - EffectKind.TARRED.rule().slowPerStack(), within(0.001f));

            effects.tick();
        }
    }

    @Test
    void aWeakerSlowOfOneKindIsIgnoredWhileTheStrongerRunsAndNeverStretchesIt() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.chill(0.6f, 20, DamageSink.none()));

        effects.apply(Effect.chill(0.1f, 160, DamageSink.none()));

        assertThat(effects.find(EffectKind.CHILL).orElseThrow().slowStrength()).isEqualTo(0.6f);
        assertThat(effects.remainingTicks(EffectKind.CHILL)).hasValue(20);
    }

    @Test
    void aStrongerSlowOfOneKindReplacesTheWeakerTailAndAll() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.chill(0.1f, 160, DamageSink.none()));

        effects.apply(Effect.chill(0.6f, 20, DamageSink.none()));

        assertThat(effects.find(EffectKind.CHILL).orElseThrow().slowStrength()).isEqualTo(0.6f);
        assertThat(effects.remainingTicks(EffectKind.CHILL)).hasValue(20);
    }

    @Test
    void ofTwoEqualSlowsOfOneKindTheLongerHolds() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.chill(0.3f, 40, DamageSink.none()));

        effects.apply(Effect.chill(0.3f, 10, DamageSink.none()));
        assertThat(effects.remainingTicks(EffectKind.CHILL)).hasValue(40);

        effects.apply(Effect.chill(0.3f, 80, DamageSink.none()));
        assertThat(effects.remainingTicks(EffectKind.CHILL)).hasValue(80);
    }

    @Test
    void ofSlowsOfDifferentKindsOnlyTheStrongestApplies() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.chill(0.7f, 100, DamageSink.none()));
        effects.apply(Effect.tarred(100, DamageSink.none()));

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.3f, within(0.001f));
    }

    @Test
    void aSlowTakesItsShareOfASpeedGain() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.hastened(100, DamageSink.none()));
        float hastened = resolved(effects, EnemyStat.MOVE_SPEED);

        effects.apply(Effect.chill(0.25f, 100, DamageSink.none()));

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(hastened * 0.75f, within(0.001f));
    }

    @Test
    void anAnchorHoldsAGainToItsCeilingOnBase() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.hastened(100, DamageSink.none()));

        effects.apply(Effect.anchored(100, DamageSink.none()));

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED))
                .isCloseTo(1f - EffectKind.ANCHORED.rule().slowPerStack(), within(0.001f));
    }

    @Test
    void aSlowWeakerThanAnAnchorAddsNothingToIt() {
        float ceiling = 1f - EffectKind.ANCHORED.rule().slowPerStack();
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.anchored(100, DamageSink.none()));

        effects.apply(Effect.chill((1f - ceiling) / 2f, 100, DamageSink.none()));

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(ceiling, within(0.001f));
    }

    @Test
    void aSlowStrongerThanAnAnchorGoesBelowIt() {
        float ceiling = 1f - EffectKind.ANCHORED.rule().slowPerStack();
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.anchored(100, DamageSink.none()));

        effects.apply(Effect.chill(1f - ceiling / 2f, 100, DamageSink.none()));

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(ceiling / 2f, within(0.001f));
    }

    @Test
    void anAnchorFadesOverTheEndOfItsTime() {
        float slow = EffectKind.ANCHORED.rule().slowPerStack();
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.anchored(2 * ActiveEffects.SLOW_FADE_TICKS, DamageSink.none()));

        tickTimes(effects, ActiveEffects.SLOW_FADE_TICKS + ActiveEffects.SLOW_FADE_TICKS / 2);

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(1f - slow / 2f, within(0.001f));
    }

    @Test
    void aStackedSlowTakesItsShareForEveryStack() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.dragged(20, 100, DamageSink.none()));

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED))
                .isCloseTo(1f - 20 * EffectKind.DRAGGED.rule().slowPerStack(), within(0.001f));
    }

    private static void tickTimes(ActiveEffects effects, int times) {
        for (int i = 0; i < times; i++) {
            effects.tick();
        }
    }

    @Test
    void aFreezeZeroesTheSpeedMultiplier() {
        ActiveEffects effects = new ActiveEffects();

        effects.apply(Effect.freeze(3, DamageSink.none()));

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isZero();
    }

    @Test
    void aFreezeStillZeroesTheSpeedOfAChilledEnemyItDoesNotConsume() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.freeze(3, DamageSink.none()));

        effects.apply(Effect.chill(0.5f, 3, DamageSink.none()));

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isZero();
    }

    @Test
    void aPoolDealsWhatItHasBurnedInAPulseOnItsFirstTickAndEveryFifthAfter() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> received = new ArrayList<>();
        int fuel = 10_000;
        effects.apply(Effect.burn(Damage.magic(fuel), recordingSink(received)));

        effects.tick();
        tickTimes(effects, 4);
        assertThat(received).containsExactly(Damage.magic(Math.round(burnedIn(fuel, 1))));

        effects.tick();
        assertThat(received).hasSize(2);
        assertThat(received.get(1).amount()).isCloseTo(Math.round(burnedIn(fuel, 6) - burnedIn(fuel, 1)), within(1));
    }

    /**
     * What a pool of {@code fuel} damage units has burned after {@code ticks}: each tick burns the same multiple of
     * the root of what is left, the one whose pool would empty in its burn time.
     */
    private static float burnedIn(float fuel, int ticks) {
        double rate = 2 * Math.sqrt(fuel) / ActiveEffects.burnTicksOf(fuel);
        double left = fuel;
        for (int t = 0; t < ticks; t++) {
            left -= Math.min(left, rate * Math.sqrt(left));
        }
        return (float) (fuel - left);
    }

    @Test
    void aPoolDealsExactlyItsFuelAndEnds() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> received = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(12_345), recordingSink(received)));

        tickTimes(effects, 600);

        assertThat(received.stream().mapToInt(Damage::amount).sum()).isCloseTo(12_345, within(1));
        assertThat(effects.has(EffectKind.BURN)).isFalse();
    }

    @Test
    void fourTimesTheFuelBurnsForTwiceAsLong() {
        int small = ticksToBurnOut(10_000);
        int large = ticksToBurnOut(40_000);

        assertThat((float) large / small).isCloseTo(2f, within(0.1f));
    }

    private static int ticksToBurnOut(int fuel) {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.burn(Damage.magic(fuel), DamageSink.none()));
        int ticks = 0;
        while (effects.has(EffectKind.BURN)) {
            effects.tick();
            ticks++;
        }
        return ticks;
    }

    @Test
    void aPoolOfLessThanAUnitBurnsAtOnce() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> received = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(1), recordingSink(received)));

        effects.tick();

        assertThat(effects.activeKinds()).doesNotContain(EffectKind.BURN);
        assertThat(received).containsExactly(Damage.magic(1));
    }

    @Test
    void fuelAddsInFullWithNoCap() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.burn(Damage.magic(1_000), DamageSink.none()));

        for (int i = 0; i < 20; i++) {
            effects.apply(Effect.burn(Damage.magic(1_000), DamageSink.none()));
        }

        assertThat(fuelOf(effects, EffectKind.BURN)).isEqualTo(21_000f);
    }

    @Test
    void fuelFromASourceAlreadyFeedingAPoolJoinsItsShare() {
        ActiveEffects effects = new ActiveEffects();
        AttackOrigin tower = AttackOrigin.fresh();
        effects.apply(Effect.burn(Damage.magic(1_000), DamageSink.none()).from(tower));

        effects.apply(Effect.burn(Damage.magic(500), DamageSink.none()).from(tower));
        effects.apply(Effect.burn(Damage.magic(500), DamageSink.none()).from(AttackOrigin.fresh()));

        assertThat(effects.find(EffectKind.BURN).orElseThrow().fuel()).extracting(FuelContribution::fuel)
                .containsExactly(1_500f, 500f);
    }

    @Test
    void aSecondTowersFuelIsCreditedToThatTowerRatherThanToTheTowerThatStartedTheBurn() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> firstTower = new ArrayList<>();
        List<Damage> secondTower = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(4_000), recordingSink(firstTower)));
        tickTimes(effects, 5);

        effects.apply(Effect.burn(Damage.magic(6_000), recordingSink(secondTower)));
        tickTimes(effects, 600);

        assertThat(total(secondTower)).isCloseTo(6_000, within(1));
        assertThat(total(firstTower) + total(secondTower)).isCloseTo(10_000, within(1));
    }

    private static int total(List<Damage> received) {
        return received.stream().mapToInt(Damage::amount).sum();
    }

    @Test
    void threeTowersFeedingOneBurnAreEachCreditedTheirOwnShareOfEveryPulse() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> firstTower = new ArrayList<>();
        List<Damage> secondTower = new ArrayList<>();
        List<Damage> thirdTower = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(6_000), recordingSink(firstTower)));
        effects.apply(Effect.burn(Damage.magic(3_000), recordingSink(secondTower)));
        effects.apply(Effect.burn(Damage.magic(1_000), recordingSink(thirdTower)));

        effects.tick();

        int pulse = Math.round(burnedIn(10_000, 1));
        assertThat(total(firstTower)).isCloseTo(Math.round(pulse * 0.6f), within(1));
        assertThat(total(secondTower)).isCloseTo(Math.round(pulse * 0.3f), within(1));
        assertThat(total(firstTower) + total(secondTower) + total(thirdTower)).isEqualTo(pulse);
    }

    @Test
    void chillAndBurnAreTrackedIndependentlyBySeparateKinds() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> received = new ArrayList<>();
        effects.apply(Effect.chill(0.5f, 5, DamageSink.none()));
        effects.apply(Effect.burn(Damage.magic(10), recordingSink(received)));

        effects.tick();

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.6f, within(0.001f));
        assertThat(received).hasSize(1);
    }

    @Test
    void aShieldAbsorbsUntilItsPoolRunsOut() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(500f, 100, DamageSink.none()));

        int first = effects.absorb(DamageType.PHYSICAL, 300, 1f);
        int second = effects.absorb(DamageType.PHYSICAL, 300, 1f);

        assertThat(first).isEqualTo(300);
        assertThat(second).isEqualTo(200);
        assertThat(effects.has(EffectKind.SHIELD)).isFalse();
        assertThat(resolved(effects, EnemyStat.PHYSICAL_DAMAGE_TAKEN)).isEqualTo(1f);
    }

    @Test
    void aShieldAbsorbsNothingOnceItExpires() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(500f, 1, DamageSink.none()));

        effects.tick();

        assertThat(effects.absorb(DamageType.PHYSICAL, 300, 1f)).isZero();
    }

    @Test
    void aLastingShieldOutlivesAnyClockShowsNoTimeLeftAndEndsOnlyWhenBroken() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(500f, Effect.LASTING_TICKS, DamageSink.none()));

        for (int tick = 0; tick < 10_000; tick++) {
            effects.tick();
        }

        assertThat(effects.remainingTicks(EffectKind.SHIELD)).isEmpty();
        assertThat(effects.absorb(DamageType.PHYSICAL, 300, 1f)).isEqualTo(300);
        assertThat(effects.absorb(DamageType.PHYSICAL, 300, 1f)).isEqualTo(200);
        assertThat(effects.has(EffectKind.SHIELD)).isFalse();
    }

    @Test
    void anInvisibilityEffectSetsFullStealthForItsDuration() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.invisible(1, DamageSink.none()));

        assertThat(resolved(effects, EnemyStat.STEALTH)).isEqualTo(1f);

        effects.tick();

        assertThat(resolved(effects, EnemyStat.STEALTH)).isZero();
    }

    @Test
    void aReappliedShieldKeepsTheLargerPoolAndTheLongerTime() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(600f, 5, DamageSink.none()));

        effects.apply(Effect.shield(200f, 10, DamageSink.none()));

        assertThat(effects.shieldGauge()).isEqualTo(new ShieldGauge(6f, 6f));
        assertThat(effects.remainingTicks(EffectKind.SHIELD)).hasValue(10);
    }

    @Test
    void shieldsFromTwoSourcesStackAndTheOneEndingSoonestTakesAHitFirst() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(300f, 50, DamageSink.none()).from(AttackOrigin.fresh()));
        effects.apply(Effect.shield(300f, 5, DamageSink.none()).from(AttackOrigin.fresh()));
        float together = effects.shieldGauge().remaining();

        effects.absorb(DamageType.PHYSICAL, 200, 1f);
        tickTimes(effects, 5);

        assertThat(together).isEqualTo(DamageUnits.inPoints(600));
        assertThat(effects.shieldGauge().remaining()).isEqualTo(DamageUnits.inPoints(300));
    }

    @Test
    void aShieldWorthMoreHoldsThatManyTimesItsPointsOfAHit() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(100f, 50, DamageSink.none()));

        int held = effects.absorb(DamageType.PHYSICAL, 500, 2f);

        assertThat(held).isEqualTo(200);
        assertThat(effects.has(EffectKind.SHIELD)).isFalse();
    }

    @Test
    void aShieldWorthNothingBreaksOnTheFirstHitItMeetsAndHoldsNothing() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(400f, 50, DamageSink.none()).withShieldRestrictedTo(DamageType.PHYSICAL));

        int magic = effects.absorb(DamageType.MAGIC, 100, 0f);
        boolean standsAfterMagic = effects.has(EffectKind.SHIELD);
        int physical = effects.absorb(DamageType.PHYSICAL, 100, 0f);

        assertThat(magic).isZero();
        assertThat(standsAfterMagic).isTrue();
        assertThat(physical).isZero();
        assertThat(effects.has(EffectKind.SHIELD)).isFalse();
    }

    @Test
    void onceAShieldThatLastsUntilBrokenBreaksTheTimedOnesLeftShowTheirTime() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(100f, Effect.LASTING_TICKS, DamageSink.none()).withShieldRestrictedTo(DamageType.MAGIC)
                .from(AttackOrigin.fresh()));
        effects.apply(Effect.shield(300f, 40, DamageSink.none()).withShieldRestrictedTo(DamageType.PHYSICAL)
                .from(AttackOrigin.fresh()));
        boolean timeShownWhileLasting = effects.remainingTicks(EffectKind.SHIELD).isPresent();

        effects.absorb(DamageType.MAGIC, 100, 1f);

        assertThat(timeShownWhileLasting).isFalse();
        assertThat(effects.remainingTicks(EffectKind.SHIELD)).hasValue(40);
    }

    @Test
    void aPhysicalShieldLetsMagicThrough() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(400f, 5, DamageSink.none()).withShieldRestrictedTo(DamageType.PHYSICAL));

        assertThat(effects.absorb(DamageType.MAGIC, 100, 1f)).isZero();
        assertThat(effects.absorb(DamageType.PHYSICAL, 100, 1f)).isEqualTo(100);
    }

    @Test
    void anUnrestrictedShieldStillAbsorbsBothDamageKinds() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(400f, 5, DamageSink.none()));

        assertThat(effects.absorb(DamageType.MAGIC, 100, 1f)).isEqualTo(100);
        assertThat(effects.absorb(DamageType.PHYSICAL, 100, 1f)).isEqualTo(100);
    }

    @Test
    void weakeningAShieldTakesItsShareOfWhatRemains() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(400f, 100, DamageSink.none()));
        effects.absorb(DamageType.PHYSICAL, 200, 1f);

        effects.weakenShield(0.25f);

        assertThat(effects.shieldGauge().remaining()).isEqualTo(1.5f);
    }

    @Test
    void aHealEffectRestoresItsPerTickAmountForItsDuration() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.heal(50, 2, DamageSink.none()));

        assertThat(resolved(effects, EnemyStat.REGENERATION)).isEqualTo(50f);

        effects.tick();
        assertThat(resolved(effects, EnemyStat.REGENERATION)).isEqualTo(50f);

        effects.tick();
        assertThat(resolved(effects, EnemyStat.REGENERATION)).isZero();
    }

    @Test
    void aRecastFromTheSameSourceKeepsTheLargerHealAndTheLaterEnd() {
        ActiveEffects effects = new ActiveEffects();
        AttackOrigin source = AttackOrigin.fresh();
        effects.apply(Effect.heal(80, 5, DamageSink.none()).from(source));

        effects.apply(Effect.heal(20, 10, DamageSink.none()).from(source));

        assertThat(resolved(effects, EnemyStat.REGENERATION)).isEqualTo(80f);
        assertThat(effects.remainingTicks(EffectKind.HEAL)).hasValue(10);
    }

    @Test
    void healsFromTwoSourcesAddUpAndEachEndsOnItsOwnClock() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.heal(80, 2, DamageSink.none()).from(AttackOrigin.fresh()));
        effects.apply(Effect.heal(20, 4, DamageSink.none()).from(AttackOrigin.fresh()));
        float together = resolved(effects, EnemyStat.REGENERATION);

        tickTimes(effects, 2);

        assertThat(together).isEqualTo(100f);
        assertThat(resolved(effects, EnemyStat.REGENERATION)).isEqualTo(20f);
        assertThat(effects.remainingTicks(EffectKind.HEAL)).hasValue(2);
    }

    @Test
    void anApplicationPastTheCapOnlyRefreshesTheSharedClock() {
        ActiveEffects effects = new ActiveEffects();
        int cap = EffectKind.VULNERABLE.maxStacks();
        effects.apply(Effect.vulnerable(cap, 80, DamageSink.none()));
        tickTimes(effects, 50);

        effects.apply(Effect.vulnerable(1, 80, DamageSink.none()));

        assertThat(effects.stacks(EffectKind.VULNERABLE)).isEqualTo(cap);
        assertThat(effects.remainingTicks(EffectKind.VULNERABLE)).hasValue(80);
    }

    @Test
    void everyStackOnASharedClockEndsTogetherWhenItRunsOut() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.vulnerable(2, 5, DamageSink.none()));

        tickTimes(effects, 5);

        assertThat(effects.stacks(EffectKind.VULNERABLE)).isZero();
        assertThat(resolved(effects, EnemyStat.PHYSICAL_DAMAGE_TAKEN)).isEqualTo(1f);
    }

    @Test
    void aRevealBeatsInvisibilityInTheStealthStat() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.invisible(100, DamageSink.none()));
        assertThat(resolved(effects, EnemyStat.STEALTH)).isEqualTo(1f);

        effects.apply(Effect.revealed(40, DamageSink.none()));

        assertThat(resolved(effects, EnemyStat.STEALTH)).isZero();
    }

    @Test
    void invisibilityShowsAgainOnceTheRevealRunsOut() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.invisible(100, DamageSink.none()));
        effects.apply(Effect.revealed(3, DamageSink.none()));

        tickTimes(effects, 3);

        assertThat(resolved(effects, EnemyStat.STEALTH)).isEqualTo(1f);
    }

    @Test
    void eachScorchedStackLowersResilienceByOneAndNoneWearOffWhileBurningKeepsEarningThem() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.burn(Damage.magic(fuelBurningFor(2_200)), DamageSink.none()));
        assertThat(resolved(effects, EnemyStat.RESILIENCE)).isEqualTo(-1f);

        tickTimes(effects, 100); // earned 1 + 10, decayed none
        assertThat(effects.stacks(EffectKind.SCORCHED)).isEqualTo(11);
        assertThat(resolved(effects, EnemyStat.RESILIENCE)).isEqualTo(-11f);

        tickTimes(effects, 2000);
        assertThat(effects.stacks(EffectKind.SCORCHED)).isBetween(99, 100);
        assertThat(resolved(effects, EnemyStat.RESILIENCE)).isBetween(-100f, -99f);
    }

    @Test
    void reapplyingABurnFeedsItsPoolButAddsNoStacks() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.burn(Damage.magic(fuelBurningFor(60)), DamageSink.none()));
        tickTimes(effects, 25); // stacks: 1, then one every 10 ticks
        int before = effects.stacks(EffectKind.BURN);

        effects.apply(Effect.burn(Damage.magic(10), DamageSink.none()));

        assertThat(effects.stacks(EffectKind.BURN)).isEqualTo(before);
    }

    @Test
    void aBurnAndAPoisonPoolStackAsSeparatePoolsEachCreditingItsOwnTower() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> burnTower = new ArrayList<>();
        List<Damage> poisonTower = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(10_000), recordingSink(burnTower)));
        effects.apply(Effect.poison(Damage.magic(4_000), recordingSink(poisonTower)));

        effects.tick();

        assertThat(burnTower).containsExactly(Damage.magic(Math.round(burnedIn(10_000, 1))));
        assertThat(poisonTower).containsExactly(Damage.magic(Math.round(burnedIn(4_000, 1))));
        assertThat(effects.activeKinds()).contains(EffectKind.BURN, EffectKind.POISON);
    }

    @Test
    void anEnemyCanBurnAgainOnceItsFreezeHasEnded() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.freeze(2, DamageSink.none()));
        tickTimes(effects, 2);

        effects.apply(Effect.burn(Damage.magic(10), DamageSink.none()));

        assertThat(effects.activeKinds()).containsExactlyInAnyOrder(EffectKind.BURN, EffectKind.SCORCHED);
        assertThat(effects.blockedKinds()).isEmpty();
    }

    @Test
    void aFreezeOnAnUnchilledEnemyIsNotExtended() {
        ActiveEffects effects = new ActiveEffects();

        effects.apply(Effect.freeze(20, DamageSink.none()));

        assertThat(effects.remainingTicks(EffectKind.FREEZE)).hasValue(20);
    }

    @Test
    void scorchedStacksOutlastTheBurnAndThenWearOffOneAtATime() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.burn(Damage.magic(fuelBurningFor(30)), DamageSink.none()));
        tickTimes(effects, 40);

        assertThat(effects.activeKinds()).containsExactly(EffectKind.SCORCHED);
        int lingering = effects.stacks(EffectKind.SCORCHED);
        assertThat(lingering).isGreaterThan(0);
        assertThat(resolved(effects, EnemyStat.RESILIENCE)).isEqualTo(-lingering);

        tickTimes(effects, 20);
        assertThat(effects.stacks(EffectKind.SCORCHED)).isEqualTo(lingering - 1);

        tickTimes(effects, 20 * lingering);
        assertThat(effects.activeKinds()).isEmpty();
        assertThat(resolved(effects, EnemyStat.RESILIENCE)).isZero();
    }

    @Test
    void aNewBurnEarnsItsFirstStackAtOnceOnTopOfTheOnesAlreadyEarned() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.burn(Damage.magic(fuelBurningFor(30)), DamageSink.none()));
        tickTimes(effects, 40);
        int earned = effects.stacks(EffectKind.SCORCHED);

        effects.apply(Effect.burn(Damage.magic(10), DamageSink.none()));

        assertThat(effects.stacks(EffectKind.SCORCHED)).isEqualTo(earned + 1);
    }

    @Test
    void sickenedStacksOutlastThePoisonAndItsSlowEndsWithThePool() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.poison(Damage.magic(fuelBurningFor(30)), DamageSink.none()));
        tickTimes(effects, 40);

        assertThat(effects.activeKinds()).containsExactly(EffectKind.SICKENED);
        assertThat(resolved(effects, EnemyStat.SPIRIT)).isEqualTo(-effects.stacks(EffectKind.SICKENED));
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isEqualTo(1f);
    }

    @Test
    void aStackDebuffNothingEarnsLosesOneStackEveryDecayInterval() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.scorched(5));

        tickTimes(effects, ActiveEffects.STACK_DECAY_INTERVAL_TICKS - 1);
        int before = effects.stacks(EffectKind.SCORCHED);
        effects.tick();

        assertThat(before).isEqualTo(5);
        assertThat(effects.stacks(EffectKind.SCORCHED)).isEqualTo(4);
    }

    /** Fuel, in damage units, that a pool burns through in about {@code ticks}. */
    private static int fuelBurningFor(int ticks) {
        float reference = 10_000f;
        double rootPerTick = Math.sqrt(reference) / ActiveEffects.burnTicksOf(reference);
        return (int) Math.round(Math.pow(ticks * rootPerTick, 2));
    }

    private static float resolvedOn(BaseStats<EnemyStat> base, ActiveEffects effects, EnemyStat stat) {
        return new StatSheet(base, effects::contributeTo).value(stat);
    }


    private static float fuelOf(ActiveEffects effects, EffectKind pool) {
        return effects.effects().stream().filter(effect -> effect.kind() == pool).findFirst()
                .map(Effect::fuelLevel).orElse(0f);
    }

    @Test
    void anyFireShowsAnInvisibleEnemyForAsLongAsItBurns() {
        ActiveEffects burning = new ActiveEffects();
        burning.apply(Effect.invisible(1000, DamageSink.none()));
        burning.apply(Effect.burn(Damage.magic(1), DamageSink.none()));
        ActiveEffects soulburning = new ActiveEffects();
        soulburning.apply(Effect.invisible(1000, DamageSink.none()));
        soulburning.apply(Effect.soulfire(Damage.magic(1), DamageSink.none()));
        float whileBurning = resolved(burning, EnemyStat.STEALTH);
        float whileSoulburning = resolved(soulburning, EnemyStat.STEALTH);

        burning.consume(EffectKind.BURN);

        assertThat(whileBurning).isZero();
        assertThat(whileSoulburning).isZero();
        assertThat(resolved(burning, EnemyStat.STEALTH)).isEqualTo(1f);
    }

    @Test
    void aFreezeLeavesTheFiresPoisonAndChillOnTheEnemy() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> dealt = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(100), dealt::add));
        effects.apply(Effect.soulfire(Damage.magic(100), DamageSink.none()));
        effects.apply(Effect.poison(Damage.magic(100), DamageSink.none()));
        effects.apply(Effect.chill(0.4f, 40, DamageSink.none()));

        effects.apply(Effect.freeze(20, DamageSink.none()));

        assertThat(effects.activeKinds()).contains(EffectKind.BURN, EffectKind.SOULFIRE, EffectKind.POISON, EffectKind.CHILL,
                EffectKind.FREEZE);
        assertThat(dealt).isEmpty();
    }

    @Test
    void aFireCatchesOnAFrozenEnemyAtItsAuthoredStrength() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.freeze(20, DamageSink.none()));

        effects.apply(Effect.burn(Damage.magic(100), DamageSink.none()));

        assertThat(effects.find(EffectKind.BURN).orElseThrow().fuelLevel()).isEqualTo(100f);
    }

    @Test
    void aRevealKeepsInvisibilityOutAndAnInvisibilityAlreadyThereWaitsItOut() {
        ActiveEffects revealedFirst = new ActiveEffects();
        revealedFirst.apply(Effect.revealed(10, DamageSink.none()));
        ActiveEffects cloakedFirst = new ActiveEffects();
        cloakedFirst.apply(Effect.invisible(20, DamageSink.none()));

        revealedFirst.apply(Effect.invisible(20, DamageSink.none()));
        cloakedFirst.apply(Effect.revealed(10, DamageSink.none()));
        float whileRevealed = resolved(cloakedFirst, EnemyStat.STEALTH);
        tickTimes(cloakedFirst, 10);

        assertThat(revealedFirst.activeKinds()).containsExactly(EffectKind.REVEALED);
        assertThat(whileRevealed).isZero();
        assertThat(resolved(cloakedFirst, EnemyStat.STEALTH)).isEqualTo(1f);
    }

    @Test
    void anInvisibleEnemyCanBeInvertedWithoutLosingItsCloak() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.invisible(20, DamageSink.none()));

        effects.apply(Effect.hex(EffectKind.INVERSION, 20, DamageSink.none()));

        assertThat(effects.activeKinds()).contains(EffectKind.INVISIBLE, EffectKind.INVERSION);
    }

    @Test
    void brittleCountsWithoutAFreeze() {
        ActiveEffects effects = new ActiveEffects();

        effects.apply(Effect.brittle(20, DamageSink.none()));

        assertThat(resolved(effects, EnemyStat.PHYSICAL_DAMAGE_TAKEN)).isGreaterThan(EnemyStat.PHYSICAL_DAMAGE_TAKEN.defaultBase());
    }

    @Test
    void aDeadZoneTakesSpiritToItsLowestAndLeavesHealsAndShieldsOn() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(0.3f, 100, DamageSink.none()));

        effects.apply(Effect.deadZone(10, DamageSink.none()));
        effects.apply(Effect.heal(5, 100, DamageSink.none()));

        assertThat(StatCurves.restoreFactor(resolved(effects, EnemyStat.SPIRIT))).isZero();
        assertThat(effects.activeKinds()).contains(EffectKind.SHIELD, EffectKind.HEAL, EffectKind.DEAD_ZONE);
    }

    @Test
    void tollLeavesEveryOtherDebuffsTimerAlone() {
        ActiveEffects plain = new ActiveEffects();
        plain.apply(Effect.exposed(100, DamageSink.none()));
        ActiveEffects tolled = new ActiveEffects();
        tolled.apply(Effect.exposed(100, DamageSink.none()));
        tolled.apply(Effect.toll(5, 1000, DamageSink.none()));

        tickTimes(plain, 60);
        tickTimes(tolled, 60);

        assertThat(tolled.remainingTicks(EffectKind.EXPOSED)).isEqualTo(plain.remainingTicks(EffectKind.EXPOSED));
    }

    @Test
    void tollNeverSlowsItself() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.toll(5, 20, DamageSink.none()));

        tickTimes(effects, 20);

        assertThat(effects.has(EffectKind.TOLL)).isFalse();
    }

    @Test
    void stacksStopAtTheKindsCapOrTheCapTheEffectWasGiven() {
        int cap = EffectKind.TOLL.maxStacks();
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.toll(cap, 20, DamageSink.none()));
        effects.apply(Effect.toll(cap, 20, DamageSink.none()));
        ActiveEffects deep = new ActiveEffects();
        deep.apply(Effect.toll(cap, 20, DamageSink.none()).withStackCap(3 * cap));
        deep.apply(Effect.toll(cap, 20, DamageSink.none()).withStackCap(3 * cap));

        assertThat(effects.stacks(EffectKind.TOLL)).isEqualTo(cap);
        assertThat(deep.stacks(EffectKind.TOLL)).isEqualTo(2 * cap);
    }

    @Test
    void aFieldEffectsStacksStopAtTheFieldsMostStrength() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.frail(FieldStrength.MOST, 10, DamageSink.none()));
        effects.apply(Effect.frail(FieldStrength.MOST, 10, DamageSink.none()));

        assertThat(effects.stacks(EffectKind.FRAIL)).isEqualTo(FieldStrength.MOST);
    }

    @Test
    void losingStacksKeepsTheClockAndLosingThemAllEndsTheEffect() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.toll(5, 30, DamageSink.none()));

        effects.loseStacks(EffectKind.TOLL, 2);
        int left = effects.stacks(EffectKind.TOLL);
        effects.loseStacks(EffectKind.TOLL, 3);

        assertThat(left).isEqualTo(3);
        assertThat(effects.has(EffectKind.TOLL)).isFalse();
    }

    @Test
    void losingAStackEndsAPool() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.burn(Damage.magic(100), DamageSink.none()));

        effects.loseStacks(EffectKind.BURN, 1);

        assertThat(effects.has(EffectKind.BURN)).isFalse();
    }

    @Test
    void consumingAnEffectEndsItOnceAndTellsWhetherThereWasOne() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.marked(100, DamageSink.none()));

        assertThat(effects.consume(EffectKind.MARKED)).isTrue();
        assertThat(effects.consume(EffectKind.MARKED)).isFalse();
    }

    @Test
    void bleedingDealsPhysicalDamageForEveryCellTravelledAndNothingWhileStopped() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> received = new ArrayList<>();
        effects.apply(Effect.bleeding(100, 100, recordingSink(received)));

        effects.tick(0.5f);
        effects.tick(0f);
        effects.tick(0.5f);

        assertThat(received).containsExactly(Damage.physical(50), Damage.physical(50));
    }

    @Test
    void bleedingCarriesTheFractionOfAUnitItCouldNotDealYet() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> received = new ArrayList<>();
        effects.apply(Effect.bleeding(10, 100, recordingSink(received)));

        for (int i = 0; i < 6; i++) {
            effects.tick(0.04f);
        }

        assertThat(received.stream().mapToInt(Damage::amount).sum()).isEqualTo(2);
    }

    @Test
    void bleedingEndsWhenItsTimeIsUp() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> received = new ArrayList<>();
        effects.apply(Effect.bleeding(100, 2, recordingSink(received)));

        effects.tick(0.5f);
        effects.tick(0.5f);

        assertThat(effects.activeKinds()).isEmpty();
        assertThat(received).hasSize(2);
    }

    @Test
    void soulfireIsAPoolOfItsOwnThatStacksWithBurnAndEarnsBothScorchedAndSickened() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> burn = new ArrayList<>();
        List<Damage> soul = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(100), recordingSink(burn)));
        effects.apply(Effect.soulfire(Damage.magic(100), recordingSink(soul)));

        effects.tick();

        assertThat(effects.activeKinds()).contains(EffectKind.BURN, EffectKind.SOULFIRE, EffectKind.SCORCHED,
                EffectKind.SICKENED);
        assertThat(burn).isNotEmpty();
        assertThat(soul).isNotEmpty();
        assertThat(effects.stacks(EffectKind.SICKENED)).isEqualTo(1);
    }

    @Test
    void aTowerCanAddScorchedStacksDirectlyAndTheyWearOffLikeAnyOther() {
        ActiveEffects effects = new ActiveEffects();

        effects.apply(Effect.scorched(3));
        effects.apply(Effect.scorched(2));

        assertThat(effects.stacks(EffectKind.SCORCHED)).isEqualTo(5);
        assertThat(resolved(effects, EnemyStat.RESILIENCE)).isEqualTo(-5f);
    }

    @Test
    void aPlainStatDebuffNeedsOnlyItsKindRow() {
        for (EffectKind kind : EffectKind.values()) {
            EffectStacking stacking = kind.rule().stacking();
            if (stacking != EffectStacking.SHARED_CLOCK && stacking != EffectStacking.EARNED) {
                continue;
            }
            ActiveEffects effects = new ActiveEffects();
            effects.apply(Effect.stackDebuff(kind, 2));
            BaseStats<EnemyStat> base = BaseStats.defaults(EnemyStat.class).with(EnemyStat.ARMOR, 50f)
                    .with(EnemyStat.MAGIC_RESIST, 50f).with(EnemyStat.MOVE_SPEED, 2f);

            StatSheet live = new StatSheet(base, effects::contributeTo);
            StatSheet row = new StatSheet(base, kind.modifiersAt(2)::applyTo);

            for (EnemyStat stat : EnemyStat.values()) {
                assertThat(live.value(stat)).as("%s %s", kind, stat).isEqualTo(row.value(stat));
            }
        }
    }

    @Test
    void aTickNamesTheEffectBehindEachDamageBeforeItLands() {
        ActiveEffects effects = new ActiveEffects();
        List<String> heard = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(100), damage -> heard.add("damage")));

        effects.tick(0f, kind -> heard.add(kind.title()));

        assertThat(heard).containsExactly(EffectKind.BURN.title(), "damage");
    }
}
