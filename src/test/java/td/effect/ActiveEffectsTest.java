package td.effect;

import org.junit.jupiter.api.Test;
import td.damage.Damage;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ActiveEffectsTest {

    private static DamageSink recordingSink(List<Damage> received) {
        return received::add;
    }

    @Test
    void withNoActiveEffectsSpeedMultiplierIsUnchanged() {
        ActiveEffects effects = new ActiveEffects();

        assertThat(effects.speedMultiplier()).isEqualTo(1f);
    }

    @Test
    void aSlowReducesTheSpeedMultiplierForItsDuration() {
        ActiveEffects effects = new ActiveEffects();

        effects.apply(Effect.slow(0.5f, 3, d -> {
        }));

        assertThat(effects.speedMultiplier()).isCloseTo(0.5f, within(0.001f));
    }

    @Test
    void aFreezeZeroesTheSpeedMultiplier() {
        ActiveEffects effects = new ActiveEffects();

        effects.apply(Effect.freeze(3, d -> {
        }));

        assertThat(effects.speedMultiplier()).isZero();
    }

    @Test
    void slowAndFreezeCombineMultiplicatively() {
        ActiveEffects effects = new ActiveEffects();

        effects.apply(Effect.slow(0.5f, 3, d -> {
        }));
        effects.apply(Effect.freeze(3, d -> {
        }));

        assertThat(effects.speedMultiplier()).isZero();
    }

    @Test
    void anEffectExpiresAfterItsDurationInTicksElapses() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.slow(0.5f, 2, d -> {
        }));

        effects.tick();
        assertThat(effects.speedMultiplier()).isCloseTo(0.5f, within(0.001f));

        effects.tick();
        assertThat(effects.speedMultiplier()).isEqualTo(1f);
    }

    @Test
    void aBurnDealsItsDamagePerTickThroughItsSinkEveryTickUntilItExpires() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> received = new ArrayList<>();
        effects.apply(Effect.burn(Damage.magic(50), 2, recordingSink(received)));

        effects.tick();
        effects.tick();
        effects.tick(); // already expired - must not fire a third time

        assertThat(received).containsExactly(Damage.magic(50), Damage.magic(50));
    }

    @Test
    void reapplyingTheSameKindAtALowerMagnitudeKeepsTheStrongerOne() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.slow(0.2f, 5, d -> {
        })); // the stronger slow (lower multiplier)

        effects.apply(Effect.slow(0.8f, 10, d -> {
        })); // weaker, but longer

        assertThat(effects.speedMultiplier()).isCloseTo(0.2f, within(0.001f));
    }

    @Test
    void reapplyingTheSameKindExtendsDurationToTheLongerOfTheTwoRegardlessOfWhichIsStronger() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.slow(0.2f, 2, d -> {
        })); // strong but short

        effects.apply(Effect.slow(0.8f, 10, d -> {
        })); // weak but long

        for (int i = 0; i < 9; i++) {
            effects.tick();
        }
        // the stronger multiplier survived at the longer duration
        assertThat(effects.speedMultiplier()).isCloseTo(0.2f, within(0.001f));

        effects.tick();
        assertThat(effects.speedMultiplier()).isEqualTo(1f);
    }

    @Test
    void slowAndBurnAreTrackedIndependentlyBySeparateKinds() {
        ActiveEffects effects = new ActiveEffects();
        List<Damage> received = new ArrayList<>();
        effects.apply(Effect.slow(0.5f, 5, d -> {
        }));
        effects.apply(Effect.burn(Damage.magic(10), 5, recordingSink(received)));

        effects.tick();

        assertThat(effects.speedMultiplier()).isCloseTo(0.5f, within(0.001f));
        assertThat(received).containsExactly(Damage.magic(10));
    }

    @Test
    void withNoActiveShieldIncomingDamageIsUnreduced() {
        ActiveEffects effects = new ActiveEffects();

        assertThat(effects.applyShield(Damage.physical(100))).isEqualTo(Damage.physical(100));
    }

    @Test
    void aShieldReducesIncomingDamageByItsPercent() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(0.4f, 5, d -> {
        }));

        assertThat(effects.applyShield(Damage.physical(100))).isEqualTo(Damage.physical(60));
    }

    @Test
    void aShieldStopsReducingDamageOnceItExpires() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(0.4f, 1, d -> {
        }));

        effects.tick();

        assertThat(effects.applyShield(Damage.physical(100))).isEqualTo(Damage.physical(100));
    }

    @Test
    void withNoActiveInvisibilityEffectIsInvisibleIsFalse() {
        ActiveEffects effects = new ActiveEffects();

        assertThat(effects.isInvisible()).isFalse();
    }

    @Test
    void anInvisibilityEffectMakesIsInvisibleTrueForItsDuration() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.invisible(1, d -> {
        }));

        assertThat(effects.isInvisible()).isTrue();

        effects.tick();

        assertThat(effects.isInvisible()).isFalse();
    }

    @Test
    void reapplyingShieldAtALowerPercentKeepsTheStrongerOne() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.shield(0.6f, 5, d -> {
        }));

        effects.apply(Effect.shield(0.2f, 10, d -> {
        }));

        assertThat(effects.applyShield(Damage.physical(100))).isEqualTo(Damage.physical(40));
    }

    @Test
    void withNoActiveHealHealPerTickIsZero() {
        ActiveEffects effects = new ActiveEffects();

        assertThat(effects.healPerTick()).isZero();
    }

    @Test
    void aHealEffectRestoresItsPerTickAmountForItsDuration() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.heal(50, 2, d -> {
        }));

        assertThat(effects.healPerTick()).isEqualTo(50);

        effects.tick();
        assertThat(effects.healPerTick()).isEqualTo(50);

        effects.tick();
        assertThat(effects.healPerTick()).isZero();
    }

    @Test
    void reapplyingHealAtALowerRateKeepsTheStrongerOne() {
        ActiveEffects effects = new ActiveEffects();
        effects.apply(Effect.heal(80, 5, d -> {
        }));

        effects.apply(Effect.heal(20, 10, d -> {
        }));

        assertThat(effects.healPerTick()).isEqualTo(80);
    }
}
