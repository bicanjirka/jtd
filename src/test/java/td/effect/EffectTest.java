package td.effect;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EffectTest {

    @Test
    void aBurnIsAFuelPool() {
        Effect burn = Effect.burn(Damage.magic(40), 60, DamageSink.none());

        assertThat(burn.dynamics()).isInstanceOf(EffectDynamics.FuelPool.class);
        assertThat(burn.damagePerTick()).isEqualTo(Damage.magic(40));
        assertThat(burn.fuelLevel()).isEqualTo(40f);
        assertThat(burn.peakL0()).isEqualTo(40f);
        assertThat(burn.stacks()).isZero();
    }

    @Test
    void aChillIsALevelOfFadingContributions() {
        Effect chill = Effect.chill(0.4f, 100, DamageSink.none());

        assertThat(chill.dynamics()).isInstanceOf(EffectDynamics.ChillLevel.class);
        assertThat(chill.fuelLevel()).isEqualTo(0.4f);
        assertThat(chill.ticksToFade()).isEqualTo(100);
        assertThat(chill.damagePerTick()).isEqualTo(Damage.none());
    }

    @Test
    void aShieldIsAPoolWithItsLimit() {
        Effect shield = Effect.shield(500f, 100, DamageSink.none()).withShieldRestrictedTo(DamageType.MAGIC);

        assertThat(shield.dynamics()).isInstanceOf(EffectDynamics.ShieldPool.class);
        assertThat(shield.shieldRemaining()).isEqualTo(500f);
        assertThat(shield.shieldMax()).isEqualTo(500f);
        assertThat(shield.shieldRestrictedTo()).contains(DamageType.MAGIC);
        assertThat(shield.fuel()).isEmpty();
    }

    @Test
    void aVulnerableIsAStackCount() {
        Effect vulnerable = Effect.vulnerable(2, 60, DamageSink.none()).withStackCap(8);

        assertThat(vulnerable.dynamics()).isInstanceOf(EffectDynamics.Stacks.class);
        assertThat(vulnerable.stacks()).isEqualTo(2);
        assertThat(vulnerable.effectiveStackCap()).isEqualTo(8);
        assertThat(vulnerable.shieldRestrictedTo()).isEqualTo(Optional.empty());
    }

    @Test
    void aFreezeIsAFlag() {
        Effect freeze = Effect.freeze(30, DamageSink.none());

        assertThat(freeze.dynamics()).isInstanceOf(EffectDynamics.Flag.class);
        assertThat(freeze.stacks()).isZero();
        assertThat(freeze.damagePerTick()).isEqualTo(Damage.none());
    }

    @Test
    void aHealRestoresEveryTick() {
        Effect heal = Effect.heal(12, 40, DamageSink.none());

        assertThat(heal.dynamics()).isInstanceOf(EffectDynamics.HealOverTime.class);
        assertThat(heal.healPerTick()).isEqualTo(12);
        assertThat(heal.damagePerTick()).isEqualTo(Damage.none());
    }

    @Test
    void anInvertedHealDealsDamageEveryTick() {
        List<Damage> dealt = new ArrayList<>();

        Effect inverted = Effect.heal(12, 40, DamageSink.none()).invertedThrough(dealt::add);
        inverted.sink().apply(inverted.damagePerTick());

        assertThat(inverted.dynamics()).isInstanceOf(EffectDynamics.TickDamage.class);
        assertThat(inverted.healPerTick()).isZero();
        assertThat(dealt).containsExactly(Damage.magic(12));
    }

    @Test
    void aBleedIsDamageForEveryCellWalked() {
        Effect bleed = Effect.bleeding(6, 80, DamageSink.none());

        assertThat(bleed.dynamics()).isEqualTo(new EffectDynamics.Distance(Damage.physical(6)));
        assertThat(bleed.damagePerTick()).isEqualTo(Damage.physical(6));
    }

    @Test
    void changingStacksOnAPoolIsAProgrammingError() {
        Effect burn = Effect.burn(Damage.magic(40), 60, DamageSink.none());

        assertThatThrownBy(() -> burn.withStacks(3, 60)).isInstanceOf(IllegalStateException.class);
    }
}
