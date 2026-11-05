package td.effect;

import td.damage.AttackOrigin;
import td.damage.Damage;
import td.damage.DamageType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

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
    }

    /** Regeneration added while it lasts: one share per source, which together add up. */
    record HealOverTime(List<HealShare> shares) implements EffectDynamics {

        public HealOverTime {
            shares = List.copyOf(shares);
        }

        /** What every share adds together, in damage units a tick. */
        public int perTick() {
            int sum = 0;
            for (HealShare share : this.shares) {
                sum += share.perTick();
            }
            return sum;
        }

        /** The ticks until the last share runs out. */
        public int remainingTicks() {
            return SourceShares.lastEnd(this.shares);
        }

        HealOverTime from(AttackOrigin source) {
            return new HealOverTime(SourceShares.keyedTo(this.shares, source));
        }

        HealOverTime with(HealOverTime incoming) {
            return new HealOverTime(SourceShares.joined(this.shares, incoming.shares));
        }

        /** Every share's time through {@code time}, without the shares then gone. */
        HealOverTime retimed(IntUnaryOperator time) {
            return new HealOverTime(SourceShares.retimed(this.shares, time));
        }
    }

    /** Shields that stack: one share per source, which together absorb what each could. */
    record ShieldPool(List<ShieldShare> shares) implements EffectDynamics {

        public ShieldPool {
            shares = List.copyOf(shares);
        }

        /** What every share has left together, in damage units. */
        public float remaining() {
            float sum = 0f;
            for (ShieldShare share : this.shares) {
                sum += share.remaining();
            }
            return sum;
        }

        /** The pools every share was cast with together, in damage units. */
        public float max() {
            float sum = 0f;
            for (ShieldShare share : this.shares) {
                sum += share.max();
            }
            return sum;
        }

        /** The ticks until the last share runs out; {@link Effect#LASTING_TICKS} while one lasts until broken. */
        public int remainingTicks() {
            return SourceShares.lastEnd(this.shares);
        }

        ShieldPool from(AttackOrigin source) {
            return new ShieldPool(SourceShares.keyedTo(this.shares, source));
        }

        ShieldPool restrictedTo(DamageType type) {
            return new ShieldPool(this.shares.stream().map(share -> share.withRestrictedTo(type)).toList());
        }

        ShieldPool with(ShieldPool incoming) {
            return new ShieldPool(SourceShares.joined(this.shares, incoming.shares));
        }

        /** Every share's time through {@code time}, without the shares then gone. */
        ShieldPool retimed(IntUnaryOperator time) {
            return new ShieldPool(SourceShares.retimed(this.shares, time));
        }

        /** Every share with {@code fraction} of what it has left taken away. */
        Optional<ShieldPool> weakened(float fraction) {
            return left(SourceShares.standing(this.shares.stream()
                    .map(share -> share.withRemaining(share.remaining() * (1f - fraction))).toList()));
        }

        /**
         * What the shares that take {@code type} hold of a hit of {@code raw} units, the one ending soonest first,
         * each holding {@code worth} times what it has left. At no worth every share the hit meets breaks and holds
         * nothing.
         */
        ShieldHit hitBy(DamageType type, int raw, float worth) {
            List<ShieldShare> after = new ArrayList<>(this.shares);
            List<Integer> order = IntStream.range(0, after.size()).filter(i -> after.get(i).takes(type)).boxed()
                    .sorted(Comparator.comparingInt(i -> after.get(i).remainingTicks())).toList();
            float rawLeft = raw;
            for (int i : order) {
                ShieldShare share = after.get(i);
                float holds = worth <= 0f ? 0f : Math.min(rawLeft, share.remaining() * worth);
                after.set(i, share.withRemaining(worth <= 0f ? 0f : share.remaining() - holds / worth));
                rawLeft -= holds;
            }
            return new ShieldHit(left(SourceShares.standing(after)), Math.round(raw - rawLeft));
        }

        private static Optional<ShieldPool> left(List<ShieldShare> standing) {
            return standing.isEmpty() ? Optional.empty() : Optional.of(new ShieldPool(standing));
        }
    }

    /**
     * What a hit left of the shields, and how many units of it they held.
     *
     * @param left     the shares still standing; empty once every one broke
     * @param absorbed the units of the hit the shields held, before anything else reduced it
     */
    record ShieldHit(Optional<ShieldPool> left, int absorbed) {
    }

    /** Damage for every cell walked: a bleed. */
    record Distance(Damage perCell) implements EffectDynamics {
    }
}
