package td.effect;

import td.damage.Damage;
import td.damage.DamageType;

import java.util.List;
import java.util.Optional;

/** The family-specific state of one live effect. */
public sealed interface EffectDynamics {

    /** On or off: freeze, invisibility, a mark, a hex, a field effect, ... */
    record Flag() implements EffectDynamics {
    }

    /** A stack count, its cap ({@code 0} for its kind's own), and whether Fault Line holds it. */
    record Stacks(int stacks, int stackCap, boolean faultLine) implements EffectDynamics {

        public Stacks withStacks(int stacks) {
            return new Stacks(stacks, this.stackCap, this.faultLine);
        }

        public Stacks withStackCap(int stackCap) {
            return new Stacks(this.stacks, stackCap, this.faultLine);
        }

        /** As Fault Line leaves a Fractured: it falls twice as far, and holds while the enemy is exposed. */
        public Stacks withFaultLine() {
            return new Stacks(this.stacks, Effect.FAULT_LINE_STACK_CAP, true);
        }
    }

    /** A chill: one fading contribution per application. */
    record ChillLevel(List<FuelContribution> fuel) implements EffectDynamics {

        public ChillLevel {
            fuel = List.copyOf(fuel);
        }

        public ChillLevel withFuel(List<FuelContribution> fuel) {
            return new ChillLevel(fuel);
        }

        /** Every contribution fading {@code factor} times as slowly. */
        public ChillLevel withDurationScaledBy(float factor) {
            return new ChillLevel(this.fuel.stream()
                    .map(c -> new FuelContribution(c.sink(), c.amount(), c.decayPerTick() / factor)).toList());
        }
    }

    /**
     * A burn, poison or Soulfire pool: one contribution per tower, the strongest application it has
     * taken (which bounds what a reapplication adds), the ticks to the next stack it earns, and what the
     * tower feeding it has made of it.
     */
    record FuelPool(Damage perTick, List<FuelContribution> fuel, float peakL0, int stackClock, PoolTuning tuning)
            implements EffectDynamics {

        public FuelPool {
            fuel = List.copyOf(fuel);
        }

        /** A new pool of {@code perTick}, all of it fed through {@code sink}, as the tower left it untuned. */
        public static FuelPool started(Damage perTick, DamageSink sink, int stackClock) {
            return new FuelPool(perTick, List.of(new FuelContribution(sink, perTick.amount())), perTick.amount(), stackClock,
                    PoolTuning.standard());
        }

        public FuelPool withFuel(List<FuelContribution> fuel, float peakL0) {
            return new FuelPool(this.perTick, fuel, peakL0, this.stackClock, this.tuning);
        }

        public FuelPool withStackClock(int stackClock) {
            return new FuelPool(this.perTick, this.fuel, this.peakL0, stackClock, this.tuning);
        }

        public FuelPool withTuning(PoolTuning tuning) {
            return new FuelPool(this.perTick, this.fuel, this.peakL0, this.stackClock, tuning);
        }

        /** This pool at {@code factor} times its strength: its damage and everything already in it. */
        public FuelPool withPoolScaledBy(float factor) {
            return new FuelPool(this.perTick.scaledBy(factor), this.fuel.stream().map(c -> c.scaledTo(c.amount() * factor)).toList(),
                    this.peakL0 * factor, this.stackClock, this.tuning);
        }
    }

    /** Health restored every tick. */
    record HealOverTime(int perTick) implements EffectDynamics {
    }

    /** Damage every tick: a heal turned against its carrier by Inversion. */
    record TickDamage(Damage perTick) implements EffectDynamics {
    }

    /** A shield's remaining points, its full size, and the damage type it is limited to. */
    record ShieldPool(float remaining, float max, Optional<DamageType> restrictedTo) implements EffectDynamics {

        public ShieldPool withShieldRemaining(float remaining) {
            return new ShieldPool(remaining, this.max, this.restrictedTo);
        }

        public ShieldPool withShieldScaledBy(float factor) {
            return new ShieldPool(this.remaining * factor, this.max * factor, this.restrictedTo);
        }

        public ShieldPool withShieldRestrictedTo(DamageType type) {
            return new ShieldPool(this.remaining, this.max, Optional.of(type));
        }
    }

    /** Damage for every cell walked: a bleed. */
    record Distance(Damage perCell) implements EffectDynamics {
    }
}
