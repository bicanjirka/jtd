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
    void aBurningFuelPoolDecaysExponentiallyMatchingTheDocumentedWorkedTable() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> received = new ArrayList<>();
        // L0 = 10, T = 60, alpha = e^(-3/60) ~= 0.9512
        effects.apply(Effect.burn(Damage.magic(10), 60, recordingSink(received)));

        effects.tick(); // L = 10.000 -> damage 10, decays to 9.512
        effects.tick(); // L = 9.512 -> damage 10, decays to 9.048
        effects.tick(); // L = 9.048 -> damage 9, decays to 8.607

        assertThat(received).containsExactly(Damage.magic(10), Damage.magic(10), Damage.magic(9));
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

        assertThat(firstTower).containsExactly(Damage.magic(10));
        assertThat(secondTower).containsExactly(Damage.magic(4));
    }

    @Test
    void aSecondTowersBurnFuelIsCreditedToThatTowerRatherThanToTheTowerThatIgnitedTheBurn() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> firstTower = new ArrayList<>();
        List<Damage> secondTower = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(10), 60, recordingSink(firstTower)));

        effects.tick(); // L = 10.000 -> 10 to the first tower, decays to 9.512

        // peakL0 stays 10 (8 < 10), so Lmax = 20; deltaL = 8 * (1 - 9.512/20) ~= 4.195
        effects.apply(Effect.burn(Damage.magic(8), 60, recordingSink(secondTower)));
        effects.tick(); // pool = 9.512 + 4.195 ~= 13.707 -> rounds to 14, split by each share

        assertThat(firstTower).containsExactly(Damage.magic(10), Damage.magic(10));
        assertThat(secondTower).containsExactly(Damage.magic(4));
    }

    @Test
    void threeTowersFuellingTheSameBurnAreEachCreditedTheirOwnProportionalShareOfOneTick() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> firstTower = new ArrayList<>();
        List<Damage> secondTower = new ArrayList<>();
        List<Damage> thirdTower = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(10), 60, recordingSink(firstTower)));
        // Lmax = 2 * max(10, 8) = 20; deltaL = 8 * (1 - 10/20) = 4; pool becomes 10 + 4 = 14
        effects.apply(Effect.burn(Damage.magic(8), 60, recordingSink(secondTower)));
        // Lmax = 2 * max(10, 6) = 20; deltaL = 6 * (1 - 14/20) = 1.8; pool becomes 14 + 1.8 = 15.8
        effects.apply(Effect.burn(Damage.magic(6), 60, recordingSink(thirdTower)));

        // total = 15.8, rounds to 16; exact shares are 10.13/4.05/1.82 - the third tower's
        // largest fractional remainder (0.82) wins the one leftover unit, giving 10/4/2 = 16
        effects.tick();

        assertThat(firstTower).containsExactly(Damage.magic(10));
        assertThat(secondTower).containsExactly(Damage.magic(4));
        assertThat(thirdTower).containsExactly(Damage.magic(2));
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
    void aShieldLowersDamageTakenByItsPercent() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(0.4f, 5, d -> {
        }));

        assertThat(resolved(effects, EnemyStat.PHYSICAL_DAMAGE_TAKEN)).isCloseTo(0.6f, within(0.001f));
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

        assertThat(resolved(effects, EnemyStat.PHYSICAL_DAMAGE_TAKEN)).isCloseTo(0.4f, within(0.001f));
    }

    @Test
    void aPhysicalOnlyShieldReducesPhysicalDamageButLeavesMagicUntouched() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(0.4f, 5, d -> {
        }).withShieldRestrictedTo(DamageType.PHYSICAL));

        assertThat(resolved(effects, EnemyStat.PHYSICAL_DAMAGE_TAKEN)).isCloseTo(0.6f, within(0.001f));
        assertThat(resolved(effects, EnemyStat.MAGIC_DAMAGE_TAKEN)).isCloseTo(1.0f, within(0.001f));
    }

    @Test
    void anUnrestrictedShieldStillAbsorbsBothDamageKinds() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(0.4f, 5, d -> {
        }));

        assertThat(resolved(effects, EnemyStat.PHYSICAL_DAMAGE_TAKEN)).isCloseTo(0.6f, within(0.001f));
        assertThat(resolved(effects, EnemyStat.MAGIC_DAMAGE_TAKEN)).isCloseTo(0.6f, within(0.001f));
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

        assertThat(burnTower).containsExactly(Damage.magic(10));
        assertThat(poisonTower).containsExactly(Damage.magic(6));
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

        assertThat(plainDamage).containsExactly(Damage.magic(100));
        assertThat(chilledDamage).containsExactly(Damage.magic(50));
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
}
