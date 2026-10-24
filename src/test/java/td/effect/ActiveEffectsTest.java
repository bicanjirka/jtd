package td.effect;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageType;
import td.stat.BaseStats;
import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;
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
    void aFreezeKeepsABurnOutAndSaysSo() {
        ActiveEffects effects = new ActiveEffects();

        effects.apply(Effect.freeze(30, DamageSink.none()));

        assertThat(effects.keptOutBy(EffectKind.BURN)).contains(EffectKind.FREEZE);
        assertThat(effects.keptOutBy(EffectKind.POISON)).isEmpty();
    }

    @Test
    void aFreshBurnIsHalfAsFullAsATopUpCanMakeIt() {
        ActiveEffects effects = new ActiveEffects();

        effects.apply(Effect.burn(Damage.magic(10), 60, DamageSink.none()));

        assertThat(effects.fullness(EffectKind.BURN).orElseThrow())
                .isCloseTo(1.0 / PoolTuning.standard().capFactor(), within(0.01));
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
    void aChillSlowsInProportionToItsLevelAndFadesLinearly() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.chill(0.4f, 100, DamageSink.none()));

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.6f, within(0.001f));

        tickTimes(effects, 50);
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.8f, within(0.001f));

        tickTimes(effects, 50);
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isEqualTo(1f);
        assertThat(effects.activeKinds()).isEmpty();
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
    void aBurningFuelPoolDealsItsDecayingDamageInAPulseEveryFiveTicks() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> received = new ArrayList<>();
        // L0 = 10, T = 60, alpha = e^(-3/60) ~= 0.9512; a pulse is L * (1 + a + a^2 + a^3 + a^4) ~= L * 4.5355
        effects.apply(Effect.burn(Damage.magic(10), 60, recordingSink(received)));

        effects.tick(); // L = 10.000 -> pulse of 45
        tickTimes(effects, 4);
        assertThat(received).containsExactly(Damage.magic(45));

        effects.tick(); // L = 10 * a^5 = 7.788 -> pulse of 35
        assertThat(received).containsExactly(Damage.magic(45), Damage.magic(35));
    }

    @Test
    void pulsingDealsAboutAsMuchInTotalAsDamagingEveryTickWould() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> received = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(100), 60, recordingSink(received)));

        tickTimes(effects, 600);

        // the sum of 100 * alpha^k over every tick, alpha = e^(-3/60)
        int total = received.stream().mapToInt(Damage::amount).sum();
        assertThat(total).isCloseTo(2050, within(60));
    }

    @Test
    void aBurningFuelPoolSelfTerminatesOnceItsRoundedDamageReachesZero() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> received = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(1), 10, recordingSink(received)));

        tickTimes(effects, 10);

        assertThat(effects.activeKinds()).doesNotContain(EffectKind.BURN);
        assertThat(received).isNotEmpty();
    }

    @Test
    void reapplyingBurnAddsFuelWithDiminishingReturnsAsThePoolNearsItsCap() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> firstTower = new ArrayList<>();
        List<Damage> secondTower = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(10), 60, recordingSink(firstTower)));

        // Lmax = 2 * max(10, 8) = 20; deltaL = 8 * (1 - 10/20) = 4; pool becomes 10 + 4 = 14
        effects.apply(Effect.burn(Damage.magic(8), 60, recordingSink(secondTower)));
        effects.tick();

        // pool = 14, pulse = 14 * 4.5355 ~= 63, split 10:4
        assertThat(firstTower).containsExactly(Damage.magic(45));
        assertThat(secondTower).containsExactly(Damage.magic(18));
    }

    @Test
    void aSecondTowersBurnFuelIsCreditedToThatTowerRatherThanToTheTowerThatIgnitedTheBurn() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> firstTower = new ArrayList<>();
        List<Damage> secondTower = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(10), 60, recordingSink(firstTower)));

        tickTimes(effects, 5); // the first pulse, 45 to the first tower; the pool decays to 7.788

        // peakL0 stays 10 (8 < 10), so Lmax = 20; deltaL = 8 * (1 - 7.788/20) ~= 4.885
        effects.apply(Effect.burn(Damage.magic(8), 60, recordingSink(secondTower)));
        effects.tick(); // pool ~= 12.673 -> pulse of 57, split by each share

        assertThat(firstTower).containsExactly(Damage.magic(45), Damage.magic(35));
        assertThat(secondTower).containsExactly(Damage.magic(22));
    }

    @Test
    void threeTowersFuellingTheSameBurnAreEachCreditedTheirOwnProportionalShareOfOnePulse() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> firstTower = new ArrayList<>();
        List<Damage> secondTower = new ArrayList<>();
        List<Damage> thirdTower = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(10), 60, recordingSink(firstTower)));
        // Lmax = 2 * max(10, 8) = 20; deltaL = 8 * (1 - 10/20) = 4; pool becomes 10 + 4 = 14
        effects.apply(Effect.burn(Damage.magic(8), 60, recordingSink(secondTower)));
        // Lmax = 2 * max(10, 6) = 20; deltaL = 6 * (1 - 14/20) = 1.8; pool becomes 14 + 1.8 = 15.8
        effects.apply(Effect.burn(Damage.magic(6), 60, recordingSink(thirdTower)));

        // total = 15.8, pulse = 15.8 * 4.5355 ~= 72; exact shares are 45.57/18.23/8.20 - the first
        // tower's largest fractional remainder (0.57) wins the one leftover unit, giving 46/18/8 = 72
        effects.tick();

        assertThat(firstTower).containsExactly(Damage.magic(46));
        assertThat(secondTower).containsExactly(Damage.magic(18));
        assertThat(thirdTower).containsExactly(Damage.magic(8));
    }

    @Test
    void chillsAddUpButNeverPastTheCapSoTheyNeverFreeze() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.chill(0.5f, 100, DamageSink.none()));
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.5f, within(0.001f));

        effects.apply(Effect.chill(0.5f, 100, DamageSink.none()));

        assertThat(effects.chillLevel()).isCloseTo(0.8f, within(0.001f));
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.2f, within(0.001f));
    }

    @Test
    void aChillHeldAtTheCapIsOnlyRefilledByWhatHasDecayed() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.chill(0.8f, 80, DamageSink.none()));
        tickTimes(effects, 20); // 0.8 - 20 * 0.01 = 0.6

        effects.apply(Effect.chill(0.8f, 80, DamageSink.none()));

        assertThat(effects.chillLevel()).isCloseTo(0.8f, within(0.001f));
    }

    @Test
    void chillAndBurnAreTrackedIndependentlyBySeparateKinds() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> received = new ArrayList<>();
        effects.apply(Effect.chill(0.5f, 5, DamageSink.none()));
        effects.apply(Effect.burn(Damage.magic(10), 5, recordingSink(received)));

        effects.tick();

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.6f, within(0.001f));
        assertThat(received).hasSize(1);
    }

    @Test
    void aShieldAbsorbsUntilItsPoolRunsOut() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(500f, 100, DamageSink.none()));

        int first = effects.absorb(DamageType.PHYSICAL, 300);
        int second = effects.absorb(DamageType.PHYSICAL, 300);

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

        assertThat(effects.absorb(DamageType.PHYSICAL, 300)).isZero();
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
    void aPhysicalShieldLetsMagicThrough() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(400f, 5, DamageSink.none()).withShieldRestrictedTo(DamageType.PHYSICAL));

        assertThat(effects.absorb(DamageType.MAGIC, 100)).isZero();
        assertThat(effects.absorb(DamageType.PHYSICAL, 100)).isEqualTo(100);
    }

    @Test
    void anUnrestrictedShieldStillAbsorbsBothDamageKinds() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(400f, 5, DamageSink.none()));

        assertThat(effects.absorb(DamageType.MAGIC, 100)).isEqualTo(100);
        assertThat(effects.absorb(DamageType.PHYSICAL, 100)).isEqualTo(100);
    }

    @Test
    void weakeningAShieldTakesItsShareOfWhatRemains() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(400f, 100, DamageSink.none()));
        effects.absorb(DamageType.PHYSICAL, 200);

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
    void reapplyingHealAtALowerRateKeepsTheStrongerOne() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.heal(80, 5, DamageSink.none()));

        effects.apply(Effect.heal(20, 10, DamageSink.none()));

        assertThat(resolved(effects, EnemyStat.REGENERATION)).isEqualTo(80f);
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
        effects.apply(Effect.burn(Damage.magic(10), 5000, DamageSink.none()));
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
        effects.apply(Effect.burn(Damage.magic(10), 5000, DamageSink.none()));
        tickTimes(effects, 25); // stacks: 1, then one every 10 ticks
        int before = effects.stacks(EffectKind.BURN);

        effects.apply(Effect.burn(Damage.magic(10), 5000, DamageSink.none()));

        assertThat(effects.stacks(EffectKind.BURN)).isEqualTo(before);
    }

    @Test
    void aPoisonPoolSlowsInProportionToWhatIsLeftOfItAndStacksWithAChill() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.poison(Damage.magic(10), 60, DamageSink.none()));
        // one application fills half the cap, so it takes 0.3 * 0.5 of the speed
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.85f, within(0.001f));

        effects.apply(Effect.chill(0.5f, 100, DamageSink.none()));

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.5f * 0.85f, within(0.001f));

        tickTimes(effects, 30);
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isGreaterThan(0.5f * 0.85f);
    }

    @Test
    void aBurnAndAPoisonPoolStackAsSeparatePoolsEachCreditingItsOwnTower() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> burnTower = new ArrayList<>();
        List<Damage> poisonTower = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(10), 60, recordingSink(burnTower)));
        effects.apply(Effect.poison(Damage.magic(6), 60, recordingSink(poisonTower)));

        effects.tick();

        assertThat(burnTower).containsExactly(Damage.magic(45));
        assertThat(poisonTower).containsExactly(Damage.magic(27));
        assertThat(effects.activeKinds()).contains(EffectKind.BURN, EffectKind.POISON);
    }

    @Test
    void freezingAnEnemyRemovesItsBurn() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.burn(Damage.magic(10), 60, DamageSink.none()));

        effects.apply(Effect.freeze(20, DamageSink.none()));

        assertThat(effects.activeKinds()).containsExactlyInAnyOrder(EffectKind.FREEZE, EffectKind.SCORCHED);
    }

    @Test
    void aFrozenEnemyCannotBeSetAlightAndNothingIsCredited() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> received = new ArrayList<>();
        effects.apply(Effect.freeze(20, DamageSink.none()));

        effects.apply(Effect.burn(Damage.magic(10), 60, recordingSink(received)));
        effects.tick();

        assertThat(effects.activeKinds()).containsExactly(EffectKind.FREEZE);
        assertThat(effects.blockedKinds()).containsExactly(EffectKind.BURN, EffectKind.SOULFIRE);
        assertThat(received).isEmpty();
    }

    @Test
    void anEnemyCanBurnAgainOnceItsFreezeHasEnded() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.freeze(2, DamageSink.none()));
        tickTimes(effects, 2);

        effects.apply(Effect.burn(Damage.magic(10), 60, DamageSink.none()));

        assertThat(effects.activeKinds()).containsExactlyInAnyOrder(EffectKind.BURN, EffectKind.SCORCHED);
        assertThat(effects.blockedKinds()).isEmpty();
    }

    @Test
    void aFreezeLeavesPoisonAloneButConsumesAChillAndLastsLongerByItsLevel() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.poison(Damage.magic(10), 60, DamageSink.none()));
        effects.apply(Effect.chill(0.5f, 100, DamageSink.none()));

        effects.apply(Effect.freeze(20, DamageSink.none()));

        assertThat(effects.activeKinds()).containsExactlyInAnyOrder(EffectKind.POISON, EffectKind.SICKENED, EffectKind.FREEZE);
        assertThat(effects.remainingTicks(EffectKind.FREEZE)).hasValue(30);
        assertThat(effects.blockedKinds()).containsExactly(EffectKind.BURN, EffectKind.SOULFIRE);
    }

    @Test
    void aFreezeOnAnUnchilledEnemyIsNotExtended() {
        ActiveEffects effects = new ActiveEffects();

        effects.apply(Effect.freeze(20, DamageSink.none()));

        assertThat(effects.remainingTicks(EffectKind.FREEZE)).hasValue(20);
    }

    @Test
    void aChilledEnemyTakesLessDamageFromItsBurn() {
        ActiveEffects plain = new ActiveEffects();
        List<Damage> plainDamage = new ArrayList<>();
        plain.apply(Effect.burn(Damage.magic(100), 60, recordingSink(plainDamage)));
        ActiveEffects chilled = new ActiveEffects();
        List<Damage> chilledDamage = new ArrayList<>();
        chilled.apply(Effect.chill(0.8f, 1000, DamageSink.none()));
        chilled.apply(Effect.burn(Damage.magic(100), 60, recordingSink(chilledDamage)));

        plain.tick();
        chilled.tick();

        assertThat(plainDamage).containsExactly(Damage.magic(454));
        assertThat(chilledDamage).containsExactly(Damage.magic(227));
    }

    @Test
    void scorchedStacksOutlastTheBurnAndThenWearOffOneAtATime() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.burn(Damage.magic(10), 30, DamageSink.none()));
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
        effects.apply(Effect.burn(Damage.magic(10), 30, DamageSink.none()));
        tickTimes(effects, 40);
        int earned = effects.stacks(EffectKind.SCORCHED);

        effects.apply(Effect.burn(Damage.magic(10), 30, DamageSink.none()));

        assertThat(effects.stacks(EffectKind.SCORCHED)).isEqualTo(earned + 1);
    }

    @Test
    void sickenedStacksOutlastThePoisonAndItsSlowEndsWithThePool() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.poison(Damage.magic(10), 30, DamageSink.none()));
        tickTimes(effects, 40);

        assertThat(effects.activeKinds()).containsExactly(EffectKind.SICKENED);
        assertThat(resolved(effects, EnemyStat.SPIRIT)).isEqualTo(-effects.stacks(EffectKind.SICKENED));
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isEqualTo(1f);
    }

    @Test
    void freezingPutsOutTheBurnButKeepsTheScorchedStacks() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.burn(Damage.magic(10), 5000, DamageSink.none()));
        tickTimes(effects, 100);
        int earned = effects.stacks(EffectKind.SCORCHED);

        effects.apply(Effect.freeze(20, DamageSink.none()));

        assertThat(effects.activeKinds()).containsExactlyInAnyOrder(EffectKind.FREEZE, EffectKind.SCORCHED);
        assertThat(effects.stacks(EffectKind.SCORCHED)).isEqualTo(earned);
        assertThat(resolved(effects, EnemyStat.RESILIENCE)).isEqualTo(-earned);
    }

    @Test
    void stackDecayFollowsTheSpiritFactorAndStopsAtZeroSpirit() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.burn(Damage.magic(10), 30, DamageSink.none()));
        tickTimes(effects, 40);
        int stacks = effects.stacks(EffectKind.SCORCHED);

        for (int i = 0; i < 200; i++) {
            effects.tick(0f);
        }
        assertThat(effects.stacks(EffectKind.SCORCHED)).isEqualTo(stacks);

        for (int i = 0; i < 10; i++) {
            effects.tick(2f);
        }
        assertThat(effects.stacks(EffectKind.SCORCHED)).isEqualTo(stacks - 1);
    }

    private static float resolvedOn(BaseStats<EnemyStat> base, ActiveEffects effects, EnemyStat stat) {
        return new StatSheet(base, effects::contributeTo).value(stat);
    }

    @Test
    void aRevealedEnemyCountsAsExposedAndBothTogetherCountOnce() {
        ActiveEffects exposed = new ActiveEffects();
        exposed.apply(Effect.exposed(100, DamageSink.none()));
        ActiveEffects both = new ActiveEffects();
        both.apply(Effect.exposed(100, DamageSink.none()));
        both.apply(Effect.revealed(100, DamageSink.none()));
        ActiveEffects revealed = new ActiveEffects();
        revealed.apply(Effect.revealed(100, DamageSink.none()));

        float once = resolved(exposed, EnemyStat.RESILIENCE);

        assertThat(once).isNegative();
        assertThat(resolved(revealed, EnemyStat.RESILIENCE)).isEqualTo(once);
        assertThat(resolved(both, EnemyStat.RESILIENCE)).isEqualTo(once);
    }

    @Test
    void faultLineLetsFracturedFallToMinusAHundredAndHoldWhileExposed() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.fractured(4, DamageSink.none()));
        effects.apply(Effect.fractured(1, DamageSink.none()).withFaultLine());
        effects.apply(Effect.fractured(9, DamageSink.none()));
        effects.apply(Effect.exposed(30, DamageSink.none()));

        assertThat(resolved(effects, EnemyStat.RESILIENCE)).isEqualTo(-100f);
        tickTimes(effects, 25);
        assertThat(resolved(effects, EnemyStat.RESILIENCE)).isEqualTo(-100f);
        tickTimes(effects, 25);
        assertThat(resolved(effects, EnemyStat.RESILIENCE)).isEqualTo(-90f);
    }

    private static float fuelOf(ActiveEffects effects, EffectKind pool) {
        return effects.effects().stream().filter(effect -> effect.kind() == pool).findFirst()
                .map(Effect::fuelLevel).orElse(0f);
    }

    @Test
    void freezingARimedEnemyLandsItsWholeBurnAtOnceAndBuysTwiceTheChillsExtraTime() {
        ActiveEffects rimed = new ActiveEffects();
        List<Damage> burnt = new ArrayList<>();
        rimed.apply(Effect.burn(Damage.magic(100), 40, burnt::add));
        rimed.apply(Effect.chill(0.4f, 40, DamageSink.none()));
        rimed.apply(Effect.hex(EffectKind.RIME, 100, DamageSink.none()));
        float burnLeft = fuelOf(rimed, EffectKind.BURN);

        rimed.apply(Effect.freeze(20, DamageSink.none()));

        assertThat(burnt).singleElement().extracting(Damage::amount)
                .isEqualTo((int) Math.round(burnLeft / (1.0 - Math.exp(-3.0 / 40))));
        assertThat(rimed.activeKinds()).doesNotContain(EffectKind.BURN);
        assertThat(rimed.remainingTicks(EffectKind.FREEZE)).hasValue(Math.round(20 * (1f + 2 * 0.4f)));
    }

    @Test
    void freezingABurningEnemyLandsHalfOfItsRemainingPoolAtOnce() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> burnt = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(100), 40, burnt::add));
        float burnLeft = fuelOf(effects, EffectKind.BURN);

        effects.apply(Effect.freeze(20, DamageSink.none()));

        assertThat(burnt).singleElement().extracting(Damage::amount)
                .isEqualTo((int) Math.round(0.5 * burnLeft / (1.0 - Math.exp(-3.0 / 40))));
        assertThat(effects.activeKinds()).doesNotContain(EffectKind.BURN);
    }

    @Test
    void aBurningInvisibleEnemyIsVisibleUntilItsPoolDecaysBelowTheFuelLevel() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.invisible(1000, DamageSink.none()));
        effects.apply(Effect.burn(Damage.magic(100), 60, DamageSink.none()));
        assertThat(resolved(effects, EnemyStat.STEALTH)).isEqualTo(0f);

        tickTimes(effects, 40);

        assertThat(effects.activeKinds()).contains(EffectKind.BURN);
        assertThat(resolved(effects, EnemyStat.STEALTH)).isEqualTo(1f);
    }

    @Test
    void aSmallBurnDoesNotRevealAnInvisibleEnemy() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.invisible(1000, DamageSink.none()));

        effects.apply(Effect.burn(Damage.magic(20), 60, DamageSink.none()));

        assertThat(resolved(effects, EnemyStat.STEALTH)).isEqualTo(1f);
    }

    @Test
    void aTarredEnemysBurnStartsAtDoubleThePoolButATopUpDoesNotDoubleAgain() {
        ActiveEffects plain = new ActiveEffects();
        ActiveEffects tarred = new ActiveEffects();
        tarred.apply(Effect.tarred(100, DamageSink.none()));
        for (ActiveEffects effects : List.of(plain, tarred)) {
            effects.apply(Effect.burn(Damage.magic(100), 60, DamageSink.none()));
        }
        assertThat(fuelOf(tarred, EffectKind.BURN)).isEqualTo(2 * fuelOf(plain, EffectKind.BURN));

        float before = fuelOf(tarred, EffectKind.BURN);
        tarred.apply(Effect.burn(Damage.magic(100), 60, DamageSink.none()));

        assertThat(fuelOf(tarred, EffectKind.BURN) - before).isLessThan(100f);
    }

    @Test
    void anAshenEnemyCannotBeFrozenAndShrugsOffThreeQuartersOfAChill() {
        ActiveEffects ashen = new ActiveEffects();
        ashen.apply(Effect.hex(EffectKind.ASH, 100, DamageSink.none()));

        ashen.apply(Effect.freeze(20, DamageSink.none()));
        ashen.apply(Effect.chill(0.4f, 40, DamageSink.none()));

        assertThat(ashen.activeKinds()).doesNotContain(EffectKind.FREEZE);
        assertThat(ashen.chillLevel()).isCloseTo(0.1f, within(1e-5f));
    }

    @Test
    void underAshAPoolHoldsTwiceAsMuchAndEarnsItsStacksTwiceAsFast() {
        ActiveEffects plain = new ActiveEffects();
        ActiveEffects ashen = new ActiveEffects();
        ashen.apply(Effect.hex(EffectKind.ASH, 1000, DamageSink.none()));
        for (ActiveEffects effects : List.of(plain, ashen)) {
            effects.apply(Effect.burn(Damage.magic(100), 200, DamageSink.none()));
            effects.apply(Effect.burn(Damage.magic(100), 200, DamageSink.none()));
        }

        tickTimes(plain, 10);
        tickTimes(ashen, 10);

        assertThat(fuelOf(ashen, EffectKind.BURN)).isGreaterThan(fuelOf(plain, EffectKind.BURN));
        assertThat(ashen.stacks(EffectKind.SCORCHED)).isEqualTo(plain.stacks(EffectKind.SCORCHED) + 1);
    }

    @Test
    void rimeAndAshEachReplaceTheOther() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.hex(EffectKind.RIME, 100, DamageSink.none()));

        effects.apply(Effect.hex(EffectKind.ASH, 100, DamageSink.none()));

        assertThat(effects.activeKinds()).containsExactly(EffectKind.ASH);
        effects.apply(Effect.hex(EffectKind.RIME, 100, DamageSink.none()));
        assertThat(effects.activeKinds()).containsExactly(EffectKind.RIME);
    }

    @Test
    void anInvertedEnemyCannotTurnInvisibleAndTheInversionRemovesAnInvisibilityItHad() {
        ActiveEffects hidden = new ActiveEffects();
        hidden.apply(Effect.invisible(100, DamageSink.none()));
        ActiveEffects inverted = new ActiveEffects();
        inverted.apply(Effect.hex(EffectKind.INVERSION, 100, DamageSink.none()));

        hidden.apply(Effect.hex(EffectKind.INVERSION, 100, DamageSink.none()));
        inverted.apply(Effect.invisible(100, DamageSink.none()));

        assertThat(hidden.activeKinds()).containsExactly(EffectKind.INVERSION);
        assertThat(inverted.activeKinds()).containsExactly(EffectKind.INVERSION);
        assertThat(inverted.blockedKinds()).contains(EffectKind.INVISIBLE);
    }

    @Test
    void aHealThatBecameDamageIsPaidEveryTickThroughItsSinkAndRestoresNothing() {
        List<Damage> paid = new ArrayList<>();
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.heal(40, 3, DamageSink.none()).invertedThrough(recordingSink(paid)));

        tickTimes(effects, 3);

        assertThat(paid).containsExactly(Damage.magic(40), Damage.magic(40), Damage.magic(40));
        assertThat(resolved(effects, EnemyStat.REGENERATION)).isZero();
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
    void deadZoneKeepsHealsAndShieldsOutButStripsNoneThatAreAlreadyThere() {
        ActiveEffects holding = new ActiveEffects();
        holding.apply(Effect.shield(0.3f, 100, DamageSink.none()));
        ActiveEffects bare = new ActiveEffects();

        holding.apply(Effect.deadZone(10, DamageSink.none()));
        bare.apply(Effect.deadZone(10, DamageSink.none()));
        holding.apply(Effect.heal(5, 100, DamageSink.none()));
        bare.apply(Effect.heal(5, 100, DamageSink.none()));
        bare.apply(Effect.shield(0.3f, 100, DamageSink.none()));

        assertThat(holding.activeKinds()).containsExactlyInAnyOrder(EffectKind.SHIELD, EffectKind.DEAD_ZONE);
        assertThat(bare.activeKinds()).containsExactly(EffectKind.DEAD_ZONE);
        assertThat(bare.blockedKinds()).contains(EffectKind.HEAL, EffectKind.SHIELD);
    }

    @Test
    void falloutKeepsHealsOutButLetsAShieldTakeHold() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.fallout(10, DamageSink.none()));

        effects.apply(Effect.heal(5, 100, DamageSink.none()));
        effects.apply(Effect.shield(0.3f, 100, DamageSink.none()));

        assertThat(effects.activeKinds()).containsExactlyInAnyOrder(EffectKind.FALLOUT, EffectKind.SHIELD);
        assertThat(effects.blockedKinds()).containsExactly(EffectKind.HEAL);
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
    void anUnveiledEnemyCannotTurnInvisibleAndKeepsItsReveal() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.revealed(40, DamageSink.none()));
        effects.apply(Effect.unveiled(40, DamageSink.none()));

        effects.apply(Effect.invisible(60, DamageSink.none()));

        assertThat(effects.activeKinds()).containsExactlyInAnyOrder(EffectKind.REVEALED, EffectKind.UNVEILED);
    }

    @Test
    void aChillFadesInAsManyTicksAsItsSlowestShareTakes() {
        Effect chill = Effect.chill(0.4f, 80, DamageSink.none());

        assertThat(chill.ticksToFade()).isEqualTo(80);
        assertThat(Effect.freeze(80, DamageSink.none()).ticksToFade()).isZero();
    }

    @Test
    void aDebuffTimerRunsAtTheSpiritPaceButNeverBelowAQuarter() {
        ActiveEffects fast = new ActiveEffects();
        fast.apply(Effect.exposed(20, DamageSink.none()));
        ActiveEffects slowest = new ActiveEffects();
        slowest.apply(Effect.exposed(20, DamageSink.none()));

        for (int i = 0; i < 10; i++) {
            fast.tick(2f);
        }
        for (int i = 0; i < 79; i++) {
            slowest.tick(0.01f);
        }

        assertThat(fast.activeKinds()).isEmpty();
        assertThat(slowest.activeKinds()).containsExactly(EffectKind.EXPOSED);
        slowest.tick(0.01f);
        assertThat(slowest.activeKinds()).isEmpty();
    }

    @Test
    void aDebuffTimerStopsWhileSpiritIsAtItsFloor() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.exposed(20, DamageSink.none()));

        for (int i = 0; i < 500; i++) {
            effects.tick(0f);
        }

        assertThat(effects.activeKinds()).containsExactly(EffectKind.EXPOSED);
        assertThat(effects.remainingTicks(EffectKind.EXPOSED)).hasValue(20);
    }

    @Test
    void aFreezeAndAnInvisibilityAreNotPacedBySpirit() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.freeze(10, DamageSink.none()));
        effects.apply(Effect.invisible(10, DamageSink.none()));

        for (int i = 0; i < 10; i++) {
            effects.tick(0f);
        }

        assertThat(effects.activeKinds()).isEmpty();
    }

    @Test
    void aChillFadesAtTheSpiritPaceToo() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.chill(0.4f, 100, DamageSink.none()));

        for (int i = 0; i < 50; i++) {
            effects.tick(2f);
        }

        assertThat(effects.activeKinds()).isEmpty();
    }

    @Test
    void shroudingARevealedEnemyEndsTheReveal() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.revealed(100, DamageSink.none()));

        effects.apply(Effect.invisible(100, DamageSink.none()));

        assertThat(effects.activeKinds()).containsExactly(EffectKind.INVISIBLE);
        assertThat(resolved(effects, EnemyStat.STEALTH)).isEqualTo(1f);
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

        effects.tick(1f, 0.5f);
        effects.tick(1f, 0f);
        effects.tick(1f, 0.5f);

        assertThat(received).containsExactly(Damage.physical(50), Damage.physical(50));
    }

    @Test
    void bleedingCarriesTheFractionOfAUnitItCouldNotDealYet() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> received = new ArrayList<>();
        effects.apply(Effect.bleeding(10, 100, recordingSink(received)));

        for (int i = 0; i < 6; i++) {
            effects.tick(1f, 0.04f);
        }

        assertThat(received.stream().mapToInt(Damage::amount).sum()).isEqualTo(2);
    }

    @Test
    void bleedingEndsWhenItsTimeIsUp() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> received = new ArrayList<>();
        effects.apply(Effect.bleeding(100, 2, recordingSink(received)));

        effects.tick(1f, 0.5f);
        effects.tick(1f, 0.5f);

        assertThat(effects.activeKinds()).isEmpty();
        assertThat(received).hasSize(2);
    }

    @Test
    void soulfireIsAPoolOfItsOwnThatStacksWithBurnAndEarnsBothScorchedAndSickened() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> burn = new ArrayList<>();
        List<Damage> soul = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(100), 60, recordingSink(burn)));
        effects.apply(Effect.soulfire(Damage.magic(100), 60, recordingSink(soul)));

        effects.tick();

        assertThat(effects.activeKinds()).contains(EffectKind.BURN, EffectKind.SOULFIRE, EffectKind.SCORCHED,
                EffectKind.SICKENED);
        assertThat(burn).isNotEmpty();
        assertThat(soul).isNotEmpty();
        assertThat(effects.stacks(EffectKind.SICKENED)).isEqualTo(1);
    }

    @Test
    void aFreezePutsOutSoulfireAndLandsHalfOfItToo() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> soul = new ArrayList<>();
        effects.apply(Effect.soulfire(Damage.magic(100), 40, recordingSink(soul)));

        effects.apply(Effect.freeze(20, DamageSink.none()));

        assertThat(soul).hasSize(1);
        assertThat(effects.activeKinds()).doesNotContain(EffectKind.SOULFIRE);
    }

    @Test
    void aPoolTunedToHoldMoreFillsFurtherBeforeItStopsTakingTopUps() {
        ActiveEffects plain = new ActiveEffects();
        ActiveEffects deep = new ActiveEffects();
        Effect tuned = Effect.burn(Damage.magic(100), 200, DamageSink.none()).withTuning(PoolTuning.standard().withCapFactor(4f));
        for (int i = 0; i < 12; i++) {
            plain.apply(Effect.burn(Damage.magic(100), 200, DamageSink.none()));
            deep.apply(tuned);
        }

        assertThat(fuelOf(deep, EffectKind.BURN)).isGreaterThan(fuelOf(plain, EffectKind.BURN) * 1.5f);
    }

    @Test
    void aPoolTunedToMarkFasterEarnsScorchedTwiceAsOften() {
        ActiveEffects plain = new ActiveEffects();
        ActiveEffects fast = new ActiveEffects();
        plain.apply(Effect.burn(Damage.magic(100), 200, DamageSink.none()));
        fast.apply(Effect.burn(Damage.magic(100), 200, DamageSink.none()).withTuning(PoolTuning.standard().withStackRate(2)));

        tickTimes(plain, 20);
        tickTimes(fast, 20);

        assertThat(fast.stacks(EffectKind.SCORCHED)).isGreaterThan(plain.stacks(EffectKind.SCORCHED) + 1);
    }

    @Test
    void aTunedPoolLandsMoreWhenAFreezePutsItOut() {
        List<Damage> plain = new ArrayList<>();
        List<Damage> shocked = new ArrayList<>();
        ActiveEffects a = new ActiveEffects();
        ActiveEffects b = new ActiveEffects();
        a.apply(Effect.burn(Damage.magic(100), 40, plain::add));
        b.apply(Effect.burn(Damage.magic(100), 40, shocked::add).withTuning(PoolTuning.standard().withFreezeBurstShare(1.5f)));

        a.apply(Effect.freeze(20, DamageSink.none()));
        b.apply(Effect.freeze(20, DamageSink.none()));

        assertThat(shocked.getFirst().amount()).isCloseTo(3 * plain.getFirst().amount(), within(2));
    }

    @Test
    void aPoolCarriesItsTunedModifiersOnlyWhileItBurns() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.burn(Damage.magic(100), 20, DamageSink.none()).withTuning(PoolTuning.standard().withCarried(StatModifiers.of(EnemyStat.PERIODIC_DAMAGE_TAKEN, StatModifier.flat(0.1f))
                .and(EnemyStat.MAGIC_DAMAGE_TAKEN, StatModifier.flat(0.15f)))));

        float whileBurning = resolved(effects, EnemyStat.PERIODIC_DAMAGE_TAKEN);
        float magicWhileBurning = resolved(effects, EnemyStat.MAGIC_DAMAGE_TAKEN);
        tickTimes(effects, 400);

        assertThat(whileBurning).isCloseTo(1.1f, within(1e-5f));
        assertThat(magicWhileBurning).isCloseTo(1.15f, within(1e-5f));
        assertThat(resolved(effects, EnemyStat.PERIODIC_DAMAGE_TAKEN)).isEqualTo(1f);
        assertThat(resolved(effects, EnemyStat.MAGIC_DAMAGE_TAKEN)).isEqualTo(1f);
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
    void brittleCountsOnlyWhileFrozen() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.brittle(100, DamageSink.none()));
        float thawed = resolved(effects, EnemyStat.PHYSICAL_DAMAGE_TAKEN);

        effects.apply(Effect.freeze(100, DamageSink.none()));

        assertThat(thawed).isEqualTo(1f);
        assertThat(resolved(effects, EnemyStat.PHYSICAL_DAMAGE_TAKEN)).isGreaterThan(thawed);
    }

    @Test
    void fracturedWearsOffAtTheFlooredPaceAndScorchedAtTheUnflooredOne() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.fractured(5, DamageSink.none()));
        effects.apply(Effect.scorched(5));

        for (int t = 0; t < 80; t++) {
            effects.tick(0.1f);
        }

        assertThat(effects.stacks(EffectKind.FRACTURED)).isEqualTo(4);
        assertThat(effects.stacks(EffectKind.SCORCHED)).isEqualTo(5);
    }

    @Test
    void aTickNamesTheEffectBehindEachDamageBeforeItLands() {
        ActiveEffects effects = new ActiveEffects();
        List<String> heard = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(100), 40, damage -> heard.add("damage")));

        effects.tick(1f, 0f, kind -> heard.add(kind.title()));

        assertThat(heard).containsExactly(EffectKind.BURN.title(), "damage");
    }

    @Test
    void aFreezeNamesTheBurnItLandsAtOnce() {
        ActiveEffects effects = new ActiveEffects();
        List<EffectKind> heard = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(100), 40, damage -> {
        }));

        effects.apply(Effect.freeze(20, DamageSink.none()), heard::add);

        assertThat(heard).containsExactly(EffectKind.BURN);
    }
}
