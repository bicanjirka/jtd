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
    void aSlowRecoversSpeedAlongAQuadraticEaseInCurveRatherThanStayingFlat() {
        ActiveEffects effects = new ActiveEffects();
        // ratio = 1 - 0.4 = 0.6, total = 100
        effects.apply(Effect.slow(0.4f, 100, d -> {
        }));

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.400f, within(0.001f)); // x = 0.00

        tickTimes(effects, 25);
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.4375f, within(0.001f)); // x = 0.25

        tickTimes(effects, 25);
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.550f, within(0.001f)); // x = 0.50

        tickTimes(effects, 25);
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.7375f, within(0.001f)); // x = 0.75

        tickTimes(effects, 25);
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isEqualTo(1f); // x = 1.00, fully recovered and expired
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
    void slowAndFreezeCombineMultiplicatively() {
        ActiveEffects effects = new ActiveEffects();

        effects.apply(Effect.slow(0.5f, 3, d -> {
        }));
        effects.apply(Effect.freeze(3, d -> {
        }));

        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isZero();
    }

    @Test
    void anEffectExpiresAfterItsDurationInTicksElapses() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.slow(0.5f, 2, d -> {
        }));

        // x = 0.5, factor = 0.25, multiplier = (1 - 0.5) + 0.5 * 0.25 = 0.625
        effects.tick();
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.625f, within(0.001f));

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

        // peakBurnL0 stays 10 (8 < 10), so Lmax = 20; deltaL = 8 * (1 - 9.512/20) ~= 4.195
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
    void reapplyingSlowAtALowerMagnitudeKeepsTheStrongerOneActiveAndDoesNotDiscardTheWeakerOne() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.slow(0.2f, 5, d -> {
        })); // the stronger slow (lower multiplier)

        effects.apply(Effect.slow(0.8f, 10, d -> {
        })); // weaker, but longer - bumped into the superseded slot, not discarded

        // x = 0 for the newly-applied winner, so its multiplier is exactly its authored minimum
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.2f, within(0.001f));
    }

    @Test
    void aSupersededSlowResumesItsOwnCurveFromWhereItsClockHasActuallyReachedOnceTheWinnerExpires() {
        ActiveEffects effects = new ActiveEffects();
        // weak but long: ratio = 0.2, total = 200 - applied first
        effects.apply(Effect.slow(0.8f, 200, d -> {
        }));
        // strong but short: ratio = 0.8, total = 20 - lands on top and becomes the winner,
        // bumping the weak slow into the superseded slot
        effects.apply(Effect.slow(0.2f, 20, d -> {
        }));

        tickTimes(effects, 20); // the strong slow's own duration completes and is removed

        // elapsed = 20, x = 20/200 = 0.1, multiplier = 0.8 + 0.2 * 0.01 = 0.802 - resumed, not
        // restarted
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.802f, within(0.001f));
    }

    @Test
    void slowAndBurnAreTrackedIndependentlyBySeparateKinds() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> received = new ArrayList<>();
        effects.apply(Effect.slow(0.5f, 5, d -> {
        }));
        effects.apply(Effect.burn(Damage.magic(10), 5, recordingSink(received)));

        effects.tick();

        // x = 0.2, factor = 0.04, multiplier = (1 - 0.5) + 0.5 * 0.04 = 0.52 - partway recovered
        assertThat(resolved(effects, EnemyStat.MOVE_SPEED)).isCloseTo(0.52f, within(0.001f));
        assertThat(received).containsExactly(Damage.magic(10));
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
}
