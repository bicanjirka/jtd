package td.effect;

import td.damage.AttackOrigin;
import td.damage.Damage;
import td.damage.DamageType;

import java.util.ArrayList;
import java.util.Comparator;
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
            int most = 0;
            for (HealShare share : this.shares) {
                most = Math.max(most, share.remainingTicks());
            }
            return most;
        }

        /** Every share keyed to {@code source}. */
        HealOverTime from(AttackOrigin source) {
            return new HealOverTime(this.shares.stream()
                    .map(share -> new HealShare(source, share.perTick(), share.remainingTicks())).toList());
        }

        /** With {@code incoming}'s shares: a source already here is refreshed, a new one is added. */
        HealOverTime with(HealOverTime incoming) {
            List<HealShare> merged = new ArrayList<>(this.shares);
            for (HealShare share : incoming.shares) {
                int at = indexOf(merged, share.source());
                if (at < 0) {
                    merged.add(share);
                } else {
                    merged.set(at, merged.get(at).refreshedBy(share));
                }
            }
            return new HealOverTime(merged);
        }

        /** A tick later, without the shares that ran out; empty once none is left. */
        Optional<HealOverTime> ticked() {
            List<HealShare> left = this.shares.stream().map(HealShare::ticked).filter(share -> !share.isOver()).toList();
            return left.isEmpty() ? Optional.empty() : Optional.of(new HealOverTime(left));
        }

        private static int indexOf(List<HealShare> shares, AttackOrigin source) {
            for (int i = 0; i < shares.size(); i++) {
                if (shares.get(i).source() == source) {
                    return i;
                }
            }
            return -1;
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
            int most = 0;
            for (ShieldShare share : this.shares) {
                most = Math.max(most, share.remainingTicks());
            }
            return most;
        }

        ShieldPool from(AttackOrigin source) {
            return new ShieldPool(this.shares.stream().map(share -> share.withSource(source)).toList());
        }

        ShieldPool restrictedTo(DamageType type) {
            return new ShieldPool(this.shares.stream().map(share -> share.withRestrictedTo(type)).toList());
        }

        /** With {@code incoming}'s shares: a source already here is topped up, a new one is added. */
        ShieldPool with(ShieldPool incoming) {
            List<ShieldShare> merged = new ArrayList<>(this.shares);
            for (ShieldShare share : incoming.shares) {
                int at = -1;
                for (int i = 0; i < merged.size(); i++) {
                    if (merged.get(i).source() == share.source()) {
                        at = i;
                    }
                }
                if (at < 0) {
                    merged.add(share);
                } else {
                    merged.set(at, merged.get(at).refreshedBy(share));
                }
            }
            return new ShieldPool(merged);
        }

        /** A tick later, without the shares that ran out; empty once none is left. */
        Optional<ShieldPool> ticked() {
            return left(this.shares.stream().map(ShieldShare::ticked).toList());
        }

        /** Every share with {@code fraction} of what it has left taken away. */
        Optional<ShieldPool> weakened(float fraction) {
            return left(this.shares.stream().map(share -> share.withRemaining(share.remaining() * (1f - fraction))).toList());
        }

        /**
         * What the shares that take {@code type} hold of a hit of {@code raw} units, the one ending soonest first,
         * each holding {@code worth} times what it has left. At no worth every share the hit meets breaks and holds
         * nothing.
         */
        ShieldHit hitBy(DamageType type, int raw, float worth) {
            List<ShieldShare> order = this.shares.stream().filter(share -> share.takes(type))
                    .sorted(Comparator.comparingInt(ShieldShare::remainingTicks)).toList();
            List<ShieldShare> after = new ArrayList<>(this.shares);
            float rawLeft = raw;
            for (ShieldShare share : order) {
                float holds = worth <= 0f ? 0f : Math.min(rawLeft, share.remaining() * worth);
                float left = worth <= 0f ? 0f : share.remaining() - holds / worth;
                after.set(after.indexOf(share), share.withRemaining(left));
                rawLeft -= holds;
            }
            return new ShieldHit(left(after), Math.round(raw - rawLeft));
        }

        private static Optional<ShieldPool> left(List<ShieldShare> shares) {
            List<ShieldShare> standing = shares.stream().filter(share -> !share.isGone()).toList();
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
