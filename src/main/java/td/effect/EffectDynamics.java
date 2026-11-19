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

    /** On or off like a flag: a freeze, and the ice it is made of. */
    record Encasing(IceKind ice) implements EffectDynamics {
    }

    /**
     * A shared count: its stacks, the ticks each stack takes to fade once nothing holds it, and whether it came from
     * a holder (an aura that holds it at its own level) rather than an application that adds.
     */
    record Stacks(int stacks, int fadeTicks, boolean held) implements EffectDynamics {

        public Stacks withStacks(int stacks) {
            return new Stacks(stacks, this.fadeTicks, this.held);
        }

        public Stacks withFadeTicks(int fadeTicks) {
            return new Stacks(this.stacks, fadeTicks, this.held);
        }
    }

    /** A stat debuff kept in shares: one share per source, each worth its own stacks on its own clock. */
    record StackShares(List<StackShare> shares) implements EffectDynamics {

        public StackShares {
            shares = List.copyOf(shares);
        }

        /** What every share is worth together, before any cap. */
        public int stacks() {
            int sum = 0;
            for (StackShare share : this.shares) {
                sum += share.stacks();
            }
            return sum;
        }

        /** The ticks until the last share runs out. */
        public int remainingTicks() {
            return SourceShares.lastEnd(this.shares);
        }

        /** The ticks until the first share runs out. */
        public int nextEnd() {
            int least = Integer.MAX_VALUE;
            for (StackShare share : this.shares) {
                least = Math.min(least, share.remainingTicks());
            }
            return this.shares.isEmpty() ? 0 : least;
        }

        StackShares from(AttackOrigin source) {
            return new StackShares(SourceShares.keyedTo(this.shares, source));
        }

        StackShares with(StackShares incoming) {
            return new StackShares(SourceShares.joined(this.shares, incoming.shares));
        }

        /** Every share's time through {@code time}, without the shares then gone. */
        StackShares retimed(IntUnaryOperator time) {
            return new StackShares(SourceShares.retimed(this.shares, time));
        }
    }

    /** A slow whose strength each application brings: the share of speed it takes away. */
    record Slow(float strength) implements EffectDynamics {
    }

    /**
     * A burn, poison or Soulfire pool: the type it deals, one contribution per source, the ticks to its next pulse,
     * and the fade step its last application set for the marks it holds.
     */
    record FuelPool(DamageType type, List<FuelContribution> fuel, int stackClock, int fadeTicks) implements EffectDynamics {

        public FuelPool {
            fuel = List.copyOf(fuel);
        }

        /** A new pool of {@code fuel}, all of it fed through {@code sink}. */
        public static FuelPool started(Damage fuel, DamageSink sink, int stackClock, int fadeTicks) {
            return new FuelPool(fuel.type(), List.of(FuelContribution.of(sink, AttackOrigin.none(), fuel.amount())),
                    stackClock, fadeTicks);
        }

        public FuelPool withFuel(List<FuelContribution> fuel) {
            return new FuelPool(this.type, fuel, this.stackClock, this.fadeTicks);
        }

        public FuelPool withStackClock(int stackClock) {
            return new FuelPool(this.type, this.fuel, stackClock, this.fadeTicks);
        }

        public FuelPool withFadeTicks(int fadeTicks) {
            return new FuelPool(this.type, this.fuel, this.stackClock, fadeTicks);
        }

        /** Every contribution put on by {@code origin}, so a source's later fuel joins its own. */
        FuelPool from(AttackOrigin origin) {
            return this.withFuel(this.fuel.stream()
                    .map(each -> new FuelContribution(each.sink(), origin, each.fuel(), each.owed())).toList());
        }

        /** Every contribution's fuel times {@code factor}, and the fade step of the marks it holds with it. */
        FuelPool scaledBy(float factor) {
            return new FuelPool(this.type, this.fuel.stream()
                    .map(each -> new FuelContribution(each.sink(), each.origin(), each.fuel() * factor, each.owed())).toList(),
                    this.stackClock, Math.max(1, Math.round(this.fadeTicks * factor)));
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

    /**
     * A slow that grows with the time it has run, so whatever shortens or lengthens it changes how far it grows.
     *
     * @param perTick the share of speed it takes away more with every tick it has run
     * @param start   the share it starts from
     * @param floor   the share it never drops below, held from what it replaced
     */
    record GrowingSlow(float perTick, float start, float floor) implements EffectDynamics {

        /** The share of speed it takes away after {@code elapsedTicks}. */
        public float strengthAfter(int elapsedTicks) {
            return Math.max(this.floor, this.start + this.perTick * elapsedTicks);
        }
    }
}
