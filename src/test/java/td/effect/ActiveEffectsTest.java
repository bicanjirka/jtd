package td.effect;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageType;
import td.stat.BaseStats;
import td.stat.EnemyStat;
import td.stat.StatSheet;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ActiveEffectsTest {

    private static float resolved(ActiveEffects effects, EnemyStat stat) {
        return new StatSheet(BaseStats.defaults().with(EnemyStat.MOVE_SPEED, 1f), effects::contributeTo).value(stat);
    }

    private static DamageSink recordingSink(List<Damage> received) {
        return received::add;
    }

    @Test
    void withNoActiveEffectsSpeedMultiplierIsUnchanged() {
        ActiveEffects effects = new ActiveEffects();

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isEqualTo(1f);
    }

    @Test
    void aChillSlowsInProportionToItsLevelAndFadesLinearly() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.chill(0.4f, 100, d -> {
        }));

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

        effects.apply(Effect.freeze(3, d -> {
        }));

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isZero();
    }

    @Test
    void aFreezeStillZeroesTheSpeedOfAChilledEnemyItDoesNotConsume() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.freeze(3, d -> {
        }));

        effects.apply(Effect.chill(0.5f, 3, d -> {
        }));

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isZero();
    }

    @Test
    void aChillEndsWhenItsLevelHasFadedToNothing() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.chill(0.5f, 2, d -> {
        }));

        effects.tick();
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.75f, within(0.001f));

        effects.tick();
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isEqualTo(1f);
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
        effects.apply(Effect.chill(0.5f, 100, d -> {
        }));
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.5f, within(0.001f));

        effects.apply(Effect.chill(0.5f, 100, d -> {
        }));

        assertThat(effects.chillLevel()).isCloseTo(0.8f, within(0.001f));
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.2f, within(0.001f));
    }

    @Test
    void aChillHeldAtTheCapIsOnlyRefilledByWhatHasDecayed() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.chill(0.8f, 80, d -> {
        }));
        tickTimes(effects, 20); // 0.8 - 20 * 0.01 = 0.6

        effects.apply(Effect.chill(0.8f, 80, d -> {
        }));

        assertThat(effects.chillLevel()).isCloseTo(0.8f, within(0.001f));
    }

    @Test
    void chillAndBurnAreTrackedIndependentlyBySeparateKinds() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> received = new ArrayList<>();
        effects.apply(Effect.chill(0.5f, 5, d -> {
        }));
        effects.apply(Effect.burn(Damage.magic(10), 5, recordingSink(received)));

        effects.tick();

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.6f, within(0.001f));
        assertThat(received).hasSize(1);
    }

    @Test
    void withNoActiveShieldDamageTakenIsUnchanged() {
        ActiveEffects effects = new ActiveEffects();

        assertThat(resolved(effects, EnemyStat.PHYSICAL_DAMAGE_TAKEN)).isCloseTo(1.0f, within(0.001f));
    }

    @Test
    void aShieldAddsShieldingEqualToItsPercent() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(0.4f, 5, d -> {
        }));

        assertThat(resolved(effects, EnemyStat.PHYSICAL_SHIELDING)).isCloseTo(0.4f, within(0.001f));
        assertThat(resolved(effects, EnemyStat.PHYSICAL_DAMAGE_TAKEN)).isEqualTo(1f);
    }

    @Test
    void aShieldStopsReducingDamageOnceItExpires() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(0.4f, 1, d -> {
        }));

        effects.tick();

        assertThat(resolved(effects, EnemyStat.PHYSICAL_DAMAGE_TAKEN)).isCloseTo(1.0f, within(0.001f));
    }

    @Test
    void withNoActiveInvisibilityEffectStealthIsZero() {
        ActiveEffects effects = new ActiveEffects();

        assertThat(resolved(effects, EnemyStat.STEALTH)).isZero();
    }

    @Test
    void anInvisibilityEffectSetsFullStealthForItsDuration() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.invisible(1, d -> {
        }));

        assertThat(resolved(effects, EnemyStat.STEALTH)).isEqualTo(1f);

        effects.tick();

        assertThat(resolved(effects, EnemyStat.STEALTH)).isZero();
    }

    @Test
    void reapplyingShieldAtALowerPercentKeepsTheStrongerOne() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(0.6f, 5, d -> {
        }));

        effects.apply(Effect.shield(0.2f, 10, d -> {
        }));

        assertThat(resolved(effects, EnemyStat.PHYSICAL_SHIELDING)).isCloseTo(0.6f, within(0.001f));
    }

    @Test
    void aPhysicalOnlyShieldReducesPhysicalDamageButLeavesMagicUntouched() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(0.4f, 5, d -> {
        }).withShieldRestrictedTo(DamageType.PHYSICAL));

        assertThat(resolved(effects, EnemyStat.PHYSICAL_SHIELDING)).isCloseTo(0.4f, within(0.001f));
        assertThat(resolved(effects, EnemyStat.MAGIC_SHIELDING)).isZero();
    }

    @Test
    void anUnrestrictedShieldStillAbsorbsBothDamageKinds() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(0.4f, 5, d -> {
        }));

        assertThat(resolved(effects, EnemyStat.PHYSICAL_SHIELDING)).isCloseTo(0.4f, within(0.001f));
        assertThat(resolved(effects, EnemyStat.MAGIC_SHIELDING)).isCloseTo(0.4f, within(0.001f));
    }

    @Test
    void withNoActiveHealRegenerationIsZero() {
        ActiveEffects effects = new ActiveEffects();

        assertThat(resolved(effects, EnemyStat.REGENERATION)).isZero();
    }

    @Test
    void aHealEffectRestoresItsPerTickAmountForItsDuration() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.heal(50, 2, d -> {
        }));

        assertThat(resolved(effects, EnemyStat.REGENERATION)).isEqualTo(50f);

        effects.tick();
        assertThat(resolved(effects, EnemyStat.REGENERATION)).isEqualTo(50f);

        effects.tick();
        assertThat(resolved(effects, EnemyStat.REGENERATION)).isZero();
    }

    @Test
    void reapplyingHealAtALowerRateKeepsTheStrongerOne() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.heal(80, 5, d -> {
        }));

        effects.apply(Effect.heal(20, 10, d -> {
        }));

        assertThat(resolved(effects, EnemyStat.REGENERATION)).isEqualTo(80f);
    }

    @Test
    void vulnerableStacksAddUpToThreeAndEachRaisesDamageTakenByAFixedShare() {
        ActiveEffects effects = new ActiveEffects();

        effects.apply(Effect.vulnerable(1, 80, d -> {
        }));
        assertThat(resolved(effects, EnemyStat.PHYSICAL_DAMAGE_TAKEN)).isCloseTo(1.15f, within(0.001f));

        effects.apply(Effect.vulnerable(1, 80, d -> {
        }));
        effects.apply(Effect.vulnerable(1, 80, d -> {
        }));

        assertThat(effects.stacks(EffectKind.VULNERABLE)).isEqualTo(3);
        assertThat(resolved(effects, EnemyStat.PHYSICAL_DAMAGE_TAKEN)).isCloseTo(1.45f, within(0.001f));
        assertThat(resolved(effects, EnemyStat.MAGIC_DAMAGE_TAKEN)).isCloseTo(1.45f, within(0.001f));
    }

    @Test
    void aFourthVulnerableApplicationOnlyRefreshesTheSharedClock() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.vulnerable(3, 80, d -> {
        }));
        tickTimes(effects, 50);

        effects.apply(Effect.vulnerable(1, 80, d -> {
        }));

        assertThat(effects.stacks(EffectKind.VULNERABLE)).isEqualTo(3);
        assertThat(effects.remainingTicks(EffectKind.VULNERABLE).getAsInt()).isEqualTo(80);
    }

    @Test
    void everyVulnerableStackEndsTogetherWhenTheSharedClockRunsOut() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.vulnerable(2, 5, d -> {
        }));

        tickTimes(effects, 5);

        assertThat(effects.stacks(EffectKind.VULNERABLE)).isZero();
        assertThat(resolved(effects, EnemyStat.PHYSICAL_DAMAGE_TAKEN)).isEqualTo(1f);
    }

    @Test
    void aRevealBeatsInvisibilityInTheStealthStat() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.invisible(100, d -> {
        }));
        assertThat(resolved(effects, EnemyStat.STEALTH)).isEqualTo(1f);

        effects.apply(Effect.revealed(40, d -> {
        }));

        assertThat(resolved(effects, EnemyStat.STEALTH)).isZero();
    }

    @Test
    void invisibilityShowsAgainOnceTheRevealRunsOut() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.invisible(100, d -> {
        }));
        effects.apply(Effect.revealed(3, d -> {
        }));

        tickTimes(effects, 3);

        assertThat(resolved(effects, EnemyStat.STEALTH)).isEqualTo(1f);
    }

    @Test
    void eachSickenedStackLowersSpiritByOneAndTheyDecayHalfAsFastAsPoisonEarnsThem() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.poison(Damage.magic(10), 5000, d -> {
        }));
        assertThat(resolved(effects, EnemyStat.SPIRIT)).isEqualTo(-1f);

        tickTimes(effects, 100); // earned 1 + 10, decayed 5
        assertThat(effects.stacks(EffectKind.SICKENED)).isEqualTo(6);
        assertThat(resolved(effects, EnemyStat.SPIRIT)).isEqualTo(-6f);

        tickTimes(effects, 2000);
        assertThat(effects.stacks(EffectKind.SICKENED)).isGreaterThan(100);
        assertThat(resolved(effects, EnemyStat.SPIRIT)).isEqualTo(-100f);
    }

    @Test
    void eachScorchedStackLowersResilienceByOneAndTheyDecayHalfAsFastAsBurningEarnsThem() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.burn(Damage.magic(10), 5000, d -> {
        }));
        assertThat(resolved(effects, EnemyStat.RESILIENCE)).isEqualTo(-1f);

        tickTimes(effects, 100); // earned 1 + 10, decayed 5
        assertThat(effects.stacks(EffectKind.SCORCHED)).isEqualTo(6);
        assertThat(resolved(effects, EnemyStat.RESILIENCE)).isEqualTo(-6f);

        tickTimes(effects, 2000);
        assertThat(resolved(effects, EnemyStat.RESILIENCE)).isEqualTo(-100f);
    }

    @Test
    void reapplyingABurnFeedsItsPoolButAddsNoStacks() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.burn(Damage.magic(10), 5000, d -> {
        }));
        tickTimes(effects, 25); // stacks: 1, then one every 10 ticks
        int before = effects.stacks(EffectKind.BURN);

        effects.apply(Effect.burn(Damage.magic(10), 5000, d -> {
        }));

        assertThat(effects.stacks(EffectKind.BURN)).isEqualTo(before);
    }

    @Test
    void aPoisonPoolSlowsInProportionToWhatIsLeftOfItAndStacksWithAChill() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.poison(Damage.magic(10), 60, d -> {
        }));
        // one application fills half the cap, so it takes 0.3 * 0.5 of the speed
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.85f, within(0.001f));

        effects.apply(Effect.chill(0.5f, 100, d -> {
        }));

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
    void aBurnNoLongerChangesCritChanceTaken() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.burn(Damage.magic(10), 60, d -> {
        }));

        assertThat(resolved(effects, EnemyStat.CRIT_CHANCE_TAKEN)).isEqualTo(1f);
    }

    @Test
    void freezingAnEnemyRemovesItsBurn() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.burn(Damage.magic(10), 60, d -> {
        }));

        effects.apply(Effect.freeze(20, d -> {
        }));

        assertThat(effects.activeKinds()).containsExactlyInAnyOrder(EffectKind.FREEZE, EffectKind.SCORCHED);
    }

    @Test
    void aFrozenEnemyCannotBeSetAlightAndNothingIsCredited() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> received = new ArrayList<>();
        effects.apply(Effect.freeze(20, d -> {
        }));

        effects.apply(Effect.burn(Damage.magic(10), 60, recordingSink(received)));
        effects.tick();

        assertThat(effects.activeKinds()).containsExactly(EffectKind.FREEZE);
        assertThat(effects.blockedKinds()).containsExactly(EffectKind.BURN);
        assertThat(received).isEmpty();
    }

    @Test
    void anEnemyCanBurnAgainOnceItsFreezeHasEnded() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.freeze(2, d -> {
        }));
        tickTimes(effects, 2);

        effects.apply(Effect.burn(Damage.magic(10), 60, d -> {
        }));

        assertThat(effects.activeKinds()).containsExactlyInAnyOrder(EffectKind.BURN, EffectKind.SCORCHED);
        assertThat(effects.blockedKinds()).isEmpty();
    }

    @Test
    void aFreezeLeavesPoisonAloneButConsumesAChillAndLastsLongerByItsLevel() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.poison(Damage.magic(10), 60, d -> {
        }));
        effects.apply(Effect.chill(0.5f, 100, d -> {
        }));

        effects.apply(Effect.freeze(20, d -> {
        }));

        assertThat(effects.activeKinds()).containsExactlyInAnyOrder(EffectKind.POISON, EffectKind.SICKENED, EffectKind.FREEZE);
        assertThat(effects.remainingTicks(EffectKind.FREEZE).getAsInt()).isEqualTo(30);
        assertThat(effects.blockedKinds()).containsExactly(EffectKind.BURN);
    }

    @Test
    void aFreezeOnAnUnchilledEnemyIsNotExtended() {
        ActiveEffects effects = new ActiveEffects();

        effects.apply(Effect.freeze(20, d -> {
        }));

        assertThat(effects.remainingTicks(EffectKind.FREEZE).getAsInt()).isEqualTo(20);
    }

    @Test
    void aChilledEnemyTakesLessDamageFromItsBurn() {
        ActiveEffects plain = new ActiveEffects();
        List<Damage> plainDamage = new ArrayList<>();
        plain.apply(Effect.burn(Damage.magic(100), 60, recordingSink(plainDamage)));
        ActiveEffects chilled = new ActiveEffects();
        List<Damage> chilledDamage = new ArrayList<>();
        chilled.apply(Effect.chill(0.8f, 1000, d -> {
        }));
        chilled.apply(Effect.burn(Damage.magic(100), 60, recordingSink(chilledDamage)));

        plain.tick();
        chilled.tick();

        assertThat(plainDamage).containsExactly(Damage.magic(454));
        assertThat(chilledDamage).containsExactly(Damage.magic(227));
    }

    @Test
    void scorchedStacksOutlastTheBurnAndThenWearOffOneAtATime() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.burn(Damage.magic(10), 30, d -> {
        }));
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
        effects.apply(Effect.burn(Damage.magic(10), 30, d -> {
        }));
        tickTimes(effects, 40);
        int earned = effects.stacks(EffectKind.SCORCHED);

        effects.apply(Effect.burn(Damage.magic(10), 30, d -> {
        }));

        assertThat(effects.stacks(EffectKind.SCORCHED)).isEqualTo(earned + 1);
    }

    @Test
    void sickenedStacksOutlastThePoisonAndItsSlowEndsWithThePool() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.poison(Damage.magic(10), 30, d -> {
        }));
        tickTimes(effects, 40);

        assertThat(effects.activeKinds()).containsExactly(EffectKind.SICKENED);
        assertThat(resolved(effects, EnemyStat.SPIRIT)).isEqualTo(-effects.stacks(EffectKind.SICKENED));
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isEqualTo(1f);
    }

    @Test
    void freezingPutsOutTheBurnButKeepsTheScorchedStacks() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.burn(Damage.magic(10), 5000, d -> {
        }));
        tickTimes(effects, 100);
        int earned = effects.stacks(EffectKind.SCORCHED);

        effects.apply(Effect.freeze(20, d -> {
        }));

        assertThat(effects.activeKinds()).containsExactlyInAnyOrder(EffectKind.FREEZE, EffectKind.SCORCHED);
        assertThat(effects.stacks(EffectKind.SCORCHED)).isEqualTo(earned);
        assertThat(resolved(effects, EnemyStat.RESILIENCE)).isEqualTo(-earned);
    }

    @Test
    void stackDecayFollowsTheSpiritFactorAndStopsAtZeroSpirit() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.burn(Damage.magic(10), 30, d -> {
        }));
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

    private static float resolvedOn(BaseStats base, ActiveEffects effects, EnemyStat stat) {
        return new StatSheet(base, effects::contributeTo).value(stat);
    }

    @Test
    void sunderedStacksEachTakeFiveArmorAndStopAtTenOnOneClock() {
        ActiveEffects effects = new ActiveEffects();
        BaseStats armored = BaseStats.defaults().with(EnemyStat.ARMOR, 80f);

        effects.apply(Effect.sundered(4, 100, d -> {
        }));
        assertThat(resolvedOn(armored, effects, EnemyStat.ARMOR)).isEqualTo(60f);

        effects.apply(Effect.sundered(20, 100, d -> {
        }));
        assertThat(effects.stacks(EffectKind.SUNDERED)).isEqualTo(10);
        assertThat(resolvedOn(armored, effects, EnemyStat.ARMOR)).isEqualTo(30f);
    }

    @Test
    void sunderedNeverTakesArmorBelowZero() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.sundered(10, 100, d -> {
        }));

        assertThat(resolvedOn(BaseStats.defaults().with(EnemyStat.ARMOR, 20f), effects, EnemyStat.ARMOR)).isEqualTo(0f);
    }

    @Test
    void exposedAndRevealedEachDoubleTheCritChanceTakenButNeverQuadrupleIt() {
        ActiveEffects exposed = new ActiveEffects();
        exposed.apply(Effect.exposed(100, d -> {
        }));
        ActiveEffects both = new ActiveEffects();
        both.apply(Effect.exposed(100, d -> {
        }));
        both.apply(Effect.revealed(100, d -> {
        }));
        ActiveEffects revealed = new ActiveEffects();
        revealed.apply(Effect.revealed(100, d -> {
        }));

        assertThat(resolved(exposed, EnemyStat.CRIT_CHANCE_TAKEN)).isEqualTo(2f);
        assertThat(resolved(revealed, EnemyStat.CRIT_CHANCE_TAKEN)).isEqualTo(2f);
        assertThat(resolved(both, EnemyStat.CRIT_CHANCE_TAKEN)).isEqualTo(2f);
    }

    @Test
    void priorityAndVulnerableMultiplyEachOtherAndResonatingOnlyRaisesMagicTaken() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.priority(100, d -> {
        }));
        effects.apply(Effect.vulnerable(1, 100, d -> {
        }));
        effects.apply(Effect.resonating(2, 100, d -> {
        }));

        assertThat(resolved(effects, EnemyStat.PHYSICAL_DAMAGE_TAKEN)).isCloseTo(1.15f * 1.15f, within(1e-5f));
        assertThat(resolved(effects, EnemyStat.MAGIC_DAMAGE_TAKEN)).isCloseTo(1.15f * 1.15f * 1.16f, within(1e-5f));
    }

    @Test
    void fracturedStacksEachTakeTenResilienceStopAtFiveAndRecoverOneASecond() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.fractured(3, d -> {
        }));
        effects.apply(Effect.fractured(9, d -> {
        }));

        assertThat(effects.stacks(EffectKind.FRACTURED)).isEqualTo(5);
        assertThat(resolved(effects, EnemyStat.RESILIENCE)).isEqualTo(-50f);

        tickTimes(effects, 20);
        assertThat(resolved(effects, EnemyStat.RESILIENCE)).isEqualTo(-40f);
        tickTimes(effects, 80);
        assertThat(effects.activeKinds()).isEmpty();
    }

    @Test
    void faultLineLetsFracturedFallToMinusAHundredAndHoldWhileExposed() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.fractured(4, d -> {
        }));
        effects.apply(Effect.fractured(1, d -> {
        }).withFaultLine());
        effects.apply(Effect.fractured(9, d -> {
        }));
        effects.apply(Effect.exposed(30, d -> {
        }));

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
        rimed.apply(Effect.chill(0.4f, 40, d -> {
        }));
        rimed.apply(Effect.hex(EffectKind.RIME, 100, d -> {
        }));
        float burnLeft = fuelOf(rimed, EffectKind.BURN);

        rimed.apply(Effect.freeze(20, d -> {
        }));

        assertThat(burnt).singleElement().extracting(Damage::amount)
                .isEqualTo((int) Math.round(burnLeft / (1.0 - Math.exp(-3.0 / 40))));
        assertThat(rimed.activeKinds()).doesNotContain(EffectKind.BURN);
        assertThat(rimed.remainingTicks(EffectKind.FREEZE).getAsInt()).isEqualTo(Math.round(20 * (1f + 2 * 0.4f)));
    }

    @Test
    void freezingABurningEnemyLandsHalfOfItsRemainingPoolAtOnce() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> burnt = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(100), 40, burnt::add));
        float burnLeft = fuelOf(effects, EffectKind.BURN);

        effects.apply(Effect.freeze(20, d -> {
        }));

        assertThat(burnt).singleElement().extracting(Damage::amount)
                .isEqualTo((int) Math.round(0.5 * burnLeft / (1.0 - Math.exp(-3.0 / 40))));
        assertThat(effects.activeKinds()).doesNotContain(EffectKind.BURN);
    }

    @Test
    void aBurningInvisibleEnemyIsVisibleUntilItsPoolDecaysBelowTheFuelLevel() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.invisible(1000, d -> {
        }));
        effects.apply(Effect.burn(Damage.magic(100), 60, d -> {
        }));
        assertThat(resolved(effects, EnemyStat.STEALTH)).isEqualTo(0f);

        tickTimes(effects, 40);

        assertThat(effects.activeKinds()).contains(EffectKind.BURN);
        assertThat(resolved(effects, EnemyStat.STEALTH)).isEqualTo(1f);
    }

    @Test
    void aSmallBurnDoesNotRevealAnInvisibleEnemy() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.invisible(1000, d -> {
        }));

        effects.apply(Effect.burn(Damage.magic(20), 60, d -> {
        }));

        assertThat(resolved(effects, EnemyStat.STEALTH)).isEqualTo(1f);
    }

    @Test
    void aTarredEnemysBurnStartsAtDoubleThePoolButATopUpDoesNotDoubleAgain() {
        ActiveEffects plain = new ActiveEffects();
        ActiveEffects tarred = new ActiveEffects();
        tarred.apply(Effect.tarred(100, d -> {
        }));
        for (ActiveEffects effects : List.of(plain, tarred)) {
            effects.apply(Effect.burn(Damage.magic(100), 60, d -> {
            }));
        }
        assertThat(fuelOf(tarred, EffectKind.BURN)).isEqualTo(2 * fuelOf(plain, EffectKind.BURN));

        float before = fuelOf(tarred, EffectKind.BURN);
        tarred.apply(Effect.burn(Damage.magic(100), 60, d -> {
        }));

        assertThat(fuelOf(tarred, EffectKind.BURN) - before).isLessThan(100f);
    }

    @Test
    void aTarredEnemyKeepsSixtyPercentOfItsSpeed() {
        ActiveEffects effects = new ActiveEffects();

        effects.apply(Effect.tarred(100, d -> {
        }));

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.6f, within(1e-5f));
    }

    @Test
    void aTarredEnemysFreezeLastsASecondLonger() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.tarred(100, d -> {
        }));

        effects.apply(Effect.freeze(20, d -> {
        }));

        assertThat(effects.remainingTicks(EffectKind.FREEZE).getAsInt()).isEqualTo(40);
    }

    @Test
    void crackedHalvesBothKindsOfPlating() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.cracked(100, d -> {
        }));
        BaseStats plated = BaseStats.defaults().with(EnemyStat.PHYSICAL_PLATING, 400f).with(EnemyStat.MAGIC_PLATING, 400f);

        StatSheet sheet = new StatSheet(plated, effects::contributeTo);

        assertThat(sheet.value(EnemyStat.PHYSICAL_PLATING)).isEqualTo(200f);
        assertThat(sheet.value(EnemyStat.MAGIC_PLATING)).isEqualTo(200f);
    }

    @Test
    void anAshenEnemyCannotBeFrozenAndShrugsOffThreeQuartersOfAChill() {
        ActiveEffects ashen = new ActiveEffects();
        ashen.apply(Effect.hex(EffectKind.ASH, 100, d -> {
        }));

        ashen.apply(Effect.freeze(20, d -> {
        }));
        ashen.apply(Effect.chill(0.4f, 40, d -> {
        }));

        assertThat(ashen.activeKinds()).doesNotContain(EffectKind.FREEZE);
        assertThat(ashen.chillLevel()).isCloseTo(0.1f, within(1e-5f));
    }

    @Test
    void underAshAPoolHoldsTwiceAsMuchAndEarnsItsStacksTwiceAsFast() {
        ActiveEffects plain = new ActiveEffects();
        ActiveEffects ashen = new ActiveEffects();
        ashen.apply(Effect.hex(EffectKind.ASH, 1000, d -> {
        }));
        for (ActiveEffects effects : List.of(plain, ashen)) {
            effects.apply(Effect.burn(Damage.magic(100), 200, d -> {
            }));
            effects.apply(Effect.burn(Damage.magic(100), 200, d -> {
            }));
        }

        tickTimes(plain, 10);
        tickTimes(ashen, 10);

        assertThat(fuelOf(ashen, EffectKind.BURN)).isGreaterThan(fuelOf(plain, EffectKind.BURN));
        assertThat(ashen.stacks(EffectKind.SCORCHED)).isEqualTo(plain.stacks(EffectKind.SCORCHED) + 1);
    }

    @Test
    void rimeAndAshEachReplaceTheOther() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.hex(EffectKind.RIME, 100, d -> {
        }));

        effects.apply(Effect.hex(EffectKind.ASH, 100, d -> {
        }));

        assertThat(effects.activeKinds()).containsExactly(EffectKind.ASH);
        effects.apply(Effect.hex(EffectKind.RIME, 100, d -> {
        }));
        assertThat(effects.activeKinds()).containsExactly(EffectKind.RIME);
    }

    @Test
    void anInvertedEnemyCannotTurnInvisibleAndTheInversionRemovesAnInvisibilityItHad() {
        ActiveEffects hidden = new ActiveEffects();
        hidden.apply(Effect.invisible(100, d -> {
        }));
        ActiveEffects inverted = new ActiveEffects();
        inverted.apply(Effect.hex(EffectKind.INVERSION, 100, d -> {
        }));

        hidden.apply(Effect.hex(EffectKind.INVERSION, 100, d -> {
        }));
        inverted.apply(Effect.invisible(100, d -> {
        }));

        assertThat(hidden.activeKinds()).containsExactly(EffectKind.INVERSION);
        assertThat(inverted.activeKinds()).containsExactly(EffectKind.INVERSION);
        assertThat(inverted.blockedKinds()).contains(EffectKind.INVISIBLE);
    }

    @Test
    void aHealThatBecameDamageIsPaidEveryTickThroughItsSinkAndRestoresNothing() {
        List<Damage> paid = new ArrayList<>();
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.heal(40, 3, d -> {
        }).invertedThrough(recordingSink(paid)));

        tickTimes(effects, 3);

        assertThat(paid).containsExactly(Damage.magic(40), Damage.magic(40), Damage.magic(40));
        assertThat(resolved(effects, EnemyStat.REGENERATION)).isZero();
    }

    @Test
    void tollSlowsEveryOtherDebuffsTimerByATenthAStack() {
        ActiveEffects plain = new ActiveEffects();
        plain.apply(Effect.exposed(100, d -> {
        }));
        ActiveEffects tolled = new ActiveEffects();
        tolled.apply(Effect.exposed(100, d -> {
        }));
        tolled.apply(Effect.toll(5, 1000, d -> {
        }));

        tickTimes(plain, 60);
        tickTimes(tolled, 60);

        assertThat(plain.remainingTicks(EffectKind.EXPOSED).getAsInt()).isEqualTo(40);
        assertThat(tolled.remainingTicks(EffectKind.EXPOSED).getAsInt()).isBetween(59, 61);
    }

    @Test
    void tollNeverSlowsItself() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.toll(5, 20, d -> {
        }));

        tickTimes(effects, 20);

        assertThat(effects.has(EffectKind.TOLL)).isFalse();
    }

    @Test
    void tollStacksUpToFiveOrTheCapItWasGiven() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.toll(4, 20, d -> {
        }));
        effects.apply(Effect.toll(4, 20, d -> {
        }));
        ActiveEffects deep = new ActiveEffects();
        deep.apply(Effect.toll(4, 20, d -> {
        }).withStackCap(10));
        deep.apply(Effect.toll(4, 20, d -> {
        }).withStackCap(10));

        assertThat(effects.stacks(EffectKind.TOLL)).isEqualTo(5);
        assertThat(deep.stacks(EffectKind.TOLL)).isEqualTo(8);
    }

    @Test
    void eachUnraveledStackLowersMagicResistByTenAndNeverBelowZero() {
        ActiveEffects effects = new ActiveEffects();
        BaseStats base = BaseStats.defaults().with(EnemyStat.MAGIC_RESIST, 25f);

        effects.apply(Effect.unraveled(2, 100, d -> {
        }));
        float two = resolvedOn(base, effects, EnemyStat.MAGIC_RESIST);
        effects.apply(Effect.unraveled(3, 100, d -> {
        }));
        float five = resolvedOn(base, effects, EnemyStat.MAGIC_RESIST);

        assertThat(two).isEqualTo(5f);
        assertThat(five).isZero();
    }

    @Test
    void brittleRaisesPhysicalDamageTakenByAThirdOnlyWhileTheEnemyIsFrozen() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.brittle(100, d -> {
        }));
        float thawed = resolved(effects, EnemyStat.PHYSICAL_DAMAGE_TAKEN);

        effects.apply(Effect.freeze(10, d -> {
        }));
        float frozen = resolved(effects, EnemyStat.PHYSICAL_DAMAGE_TAKEN);

        assertThat(thawed).isEqualTo(1f);
        assertThat(frozen).isCloseTo(1.3f, within(0.001f));
        assertThat(resolved(effects, EnemyStat.MAGIC_DAMAGE_TAKEN)).isEqualTo(1f);
    }

    @Test
    void anchoredCapsSpeedAtThreeQuartersOfBaseAndLeavesASlowerEnemyAlone() {
        ActiveEffects fast = new ActiveEffects();
        fast.apply(Effect.anchored(100, d -> {
        }));
        ActiveEffects slowed = new ActiveEffects();
        slowed.apply(Effect.anchored(100, d -> {
        }));
        slowed.apply(Effect.chill(0.5f, 100, d -> {
        }));
        BaseStats base = BaseStats.defaults().with(EnemyStat.MOVE_SPEED, 2f);

        assertThat(resolvedOn(base, fast, EnemyStat.MOVE_SPEED)).isCloseTo(1.5f, within(0.001f));
        assertThat(resolvedOn(base, slowed, EnemyStat.MOVE_SPEED)).isCloseTo(1f, within(0.001f));
    }

    @Test
    void corrosionLowersArmorByThirtyAndNeverBelowZero() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.corroded(10, d -> {
        }));

        assertThat(resolvedOn(BaseStats.defaults().with(EnemyStat.ARMOR, 50f), effects, EnemyStat.ARMOR))
                .isEqualTo(20f);
        assertThat(resolvedOn(BaseStats.defaults().with(EnemyStat.ARMOR, 10f), effects, EnemyStat.ARMOR)).isZero();
    }

    @Test
    void appliedSickenedStacksAddUpLowerSpiritByOneEachAndStopAtAHundred() {
        ActiveEffects effects = new ActiveEffects();

        effects.apply(Effect.sickened(5));
        effects.apply(Effect.sickened(5));
        float ten = resolved(effects, EnemyStat.SPIRIT);
        for (int i = 0; i < 30; i++) {
            effects.apply(Effect.sickened(5));
        }

        assertThat(ten).isEqualTo(-10f);
        assertThat(effects.stacks(EffectKind.SICKENED)).isEqualTo(100);
        assertThat(resolved(effects, EnemyStat.SPIRIT)).isEqualTo(-100f);
    }

    @Test
    void killZoneRaisesEveryDamageTakenByAQuarter() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.killZone(10, d -> {
        }));

        assertThat(resolved(effects, EnemyStat.PHYSICAL_DAMAGE_TAKEN)).isCloseTo(1.25f, within(0.001f));
        assertThat(resolved(effects, EnemyStat.MAGIC_DAMAGE_TAKEN)).isCloseTo(1.25f, within(0.001f));
    }

    @Test
    void deadZoneKeepsHealsAndShieldsOutButStripsNoneThatAreAlreadyThere() {
        ActiveEffects holding = new ActiveEffects();
        holding.apply(Effect.shield(0.3f, 100, d -> {
        }));
        ActiveEffects bare = new ActiveEffects();

        holding.apply(Effect.deadZone(10, d -> {
        }));
        bare.apply(Effect.deadZone(10, d -> {
        }));
        holding.apply(Effect.heal(5, 100, d -> {
        }));
        bare.apply(Effect.heal(5, 100, d -> {
        }));
        bare.apply(Effect.shield(0.3f, 100, d -> {
        }));

        assertThat(holding.activeKinds()).containsExactlyInAnyOrder(EffectKind.SHIELD, EffectKind.DEAD_ZONE);
        assertThat(bare.activeKinds()).containsExactly(EffectKind.DEAD_ZONE);
        assertThat(bare.blockedKinds()).contains(EffectKind.HEAL, EffectKind.SHIELD);
    }

    @Test
    void undertowChillsAQuarterAndBuysAFreezeTheChillsExtraTime() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.undertow(10, d -> {
        }));
        BaseStats base = BaseStats.defaults().with(EnemyStat.MOVE_SPEED, 2f);

        float slowed = resolvedOn(base, effects, EnemyStat.MOVE_SPEED);
        effects.apply(Effect.freeze(40, d -> {
        }));

        assertThat(slowed).isCloseTo(1.5f, within(0.001f));
        assertThat(effects.remainingTicks(EffectKind.FREEZE).getAsInt()).isEqualTo(50);
        assertThat(effects.has(EffectKind.UNDERTOW)).isTrue();
    }

    @Test
    void aChillFadesInAsManyTicksAsItsSlowestShareTakes() {
        Effect chill = Effect.chill(0.4f, 80, d -> {
        });

        assertThat(chill.ticksToFade()).isEqualTo(80);
        assertThat(Effect.freeze(80, d -> {
        }).ticksToFade()).isZero();
    }

    @Test
    void aDebuffTimerRunsAtTheSpiritPaceButNeverBelowAQuarter() {
        ActiveEffects fast = new ActiveEffects();
        fast.apply(Effect.exposed(20, d -> {
        }));
        ActiveEffects slowest = new ActiveEffects();
        slowest.apply(Effect.exposed(20, d -> {
        }));

        for (int i = 0; i < 10; i++) {
            fast.tick(2f);
        }
        for (int i = 0; i < 79; i++) {
            slowest.tick(0f);
        }

        assertThat(fast.activeKinds()).isEmpty();
        assertThat(slowest.activeKinds()).containsExactly(EffectKind.EXPOSED);
        slowest.tick(0f);
        assertThat(slowest.activeKinds()).isEmpty();
    }

    @Test
    void aFreezeAndAnInvisibilityAreNotPacedBySpirit() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.freeze(10, d -> {
        }));
        effects.apply(Effect.invisible(10, d -> {
        }));

        for (int i = 0; i < 10; i++) {
            effects.tick(0f);
        }

        assertThat(effects.activeKinds()).isEmpty();
    }

    @Test
    void aChillFadesAtTheSpiritPaceToo() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.chill(0.4f, 100, d -> {
        }));

        for (int i = 0; i < 50; i++) {
            effects.tick(2f);
        }

        assertThat(effects.activeKinds()).isEmpty();
    }

    @Test
    void shroudingARevealedEnemyEndsTheReveal() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.revealed(100, d -> {
        }));

        effects.apply(Effect.invisible(100, d -> {
        }));

        assertThat(effects.activeKinds()).containsExactly(EffectKind.INVISIBLE);
        assertThat(resolved(effects, EnemyStat.STEALTH)).isEqualTo(1f);
    }

    @Test
    void consumingAnEffectEndsItOnceAndTellsWhetherThereWasOne() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.marked(100, d -> {
        }));

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
}
