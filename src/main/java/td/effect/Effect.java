package td.effect;

import td.damage.AttackOrigin;
import td.damage.Damage;
import td.damage.DamageType;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * A timed modifier on a live enemy, from a tower hit or an enemy ability: what every effect has (its
 * kind, the time it has left, the time it was authored for, the sink its damage goes through, and who
 * put it on), plus the state of its family ({@link EffectDynamics}), which carries only its own data.
 * <p>
 * {@link #authoredDurationTicks} never counts down, so progress through the effect's life can be
 * computed. {@link #origin} is who put it on, for an effect that must tell its applier from other
 * attackers. The family accessors below give the identity value for an effect of another family.
 */
public record Effect(EffectKind kind, int remainingTicks, int authoredDurationTicks, DamageSink sink,
                     AttackOrigin origin, EffectDynamics dynamics) {

    /** A heal's or a shield's time is always its last share's, so the two can never disagree. */
    public Effect {
        remainingTicks = switch (dynamics) {
            case EffectDynamics.HealOverTime heal -> heal.remainingTicks();
            case EffectDynamics.ShieldPool shield -> shield.remainingTicks();
            default -> remainingTicks;
        };
    }

    /** A burn or poison earns a stack this often while it lasts: one every half second. */
    static final int STACK_INTERVAL_TICKS = 10;
    /** A discharged charge lands this share of the hit that discharged it. */
    public static final float CHARGE_DISCHARGE_SHARE = 0.3f;
    /** Spirit runs from 0 down to -100, so a hundred stacks of Sickened is all there is to lose. */
    private static final int SICKENED_STACK_CAP = 100;
    /** A duration that never runs out: the effect holds until it breaks, something removes it, or its carrier dies. */
    public static final int LASTING_TICKS = Integer.MAX_VALUE;
    private static final EffectDynamics FLAG = new EffectDynamics.Flag();

    /** Takes {@code strength} of base speed away for {@code durationTicks} (0.5 is half speed). */
    public static Effect chill(float strength, int durationTicks, DamageSink sink) {
        return of(EffectKind.CHILL, durationTicks, sink, new EffectDynamics.Slow(strength));
    }

    public static Effect freeze(int durationTicks, DamageSink sink) {
        return freeze(durationTicks, IceKind.FROST, sink);
    }

    /** A freeze of {@code ice}: one in ruby also holds what is immune to freezing. */
    public static Effect freeze(int durationTicks, IceKind ice, DamageSink sink) {
        return of(EffectKind.FREEZE, durationTicks, sink, new EffectDynamics.Encasing(ice));
    }

    /** Stops the enemy for {@code durationTicks}. */
    public static Effect dazed(int durationTicks, DamageSink sink) {
        return timed(EffectKind.DAZED, durationTicks, sink);
    }

    /**
     * {@code fuel} for the enemy's burn, which deals all of it over a time that grows with what the pool holds; each
     * stack it earns lowers resilience by one for good.
     */
    public static Effect burn(Damage fuel, DamageSink sink) {
        return pool(EffectKind.BURN, fuel, sink);
    }

    /**
     * {@code fuel} for a second pool of its own, so it stacks with a burn: damage, and each stack it earns lowers
     * spirit by one for good.
     */
    public static Effect poison(Damage fuel, DamageSink sink) {
        return pool(EffectKind.POISON, fuel, sink);
    }

    /**
     * {@code fuel} for a third pool of its own: it stacks with burn and poison, burns what is immune to fire, and earns
     * Sickened as well as Scorched.
     */
    public static Effect soulfire(Damage fuel, DamageSink sink) {
        return pool(EffectKind.SOULFIRE, fuel, sink);
    }

    /** A pool has no clock: it lasts until its fuel is burned. */
    private static Effect pool(EffectKind kind, Damage fuel, DamageSink sink) {
        return of(kind, LASTING_TICKS, sink, EffectDynamics.FuelPool.started(fuel, sink, STACK_INTERVAL_TICKS));
    }

    /**
     * Absorbs up to {@code units} of damage while active, then breaks. Shields from different sources ({@link #from})
     * stack.
     */
    public static Effect shield(float units, int durationTicks, DamageSink sink) {
        return of(EffectKind.SHIELD, durationTicks, sink, new EffectDynamics.ShieldPool(
                List.of(ShieldShare.cast(units, durationTicks))));
    }

    /** Untargetable while active. */
    public static Effect invisible(int durationTicks, DamageSink sink) {
        return timed(EffectKind.INVISIBLE, durationTicks, sink);
    }

    /** Faster while active. */
    public static Effect sprinting(int durationTicks, DamageSink sink) {
        return timed(EffectKind.SPRINTING, durationTicks, sink);
    }

    /** Twice as fast while active. */
    public static Effect hastened(int durationTicks, DamageSink sink) {
        return timed(EffectKind.HASTENED, durationTicks, sink);
    }

    /** {@code stacks} more stacks of magic resist, on one shared clock. */
    public static Effect spellproof(int stacks, int durationTicks, DamageSink sink) {
        return stacking(EffectKind.SPELLPROOF, stacks, durationTicks, sink);
    }

    /**
     * {@code stacks} of less damage taken, which its own applications hold at {@code stacks} however often they
     * come: the cap is what it brings.
     */
    public static Effect resolve(int stacks, int durationTicks, DamageSink sink) {
        return stacking(EffectKind.RESOLVE, stacks, durationTicks, sink).withStackCap(stacks);
    }

    /** Far more resilience, spirit and magic resist while active. */
    public static Effect unyielding(int durationTicks, DamageSink sink) {
        return timed(EffectKind.UNYIELDING, durationTicks, sink);
    }

    /** Targetable again while active, even through invisibility, which it also keeps out. */
    public static Effect revealed(int durationTicks, DamageSink sink) {
        return timed(EffectKind.REVEALED, durationTicks, sink);
    }

    /**
     * A stack debuff with {@code stacks} stacks that wear off one at a time: a burn's or poison's lasting mark, made by
     * {@code ActiveEffects} as a pool earns them, or Toll.
     */
    static Effect stackDebuff(EffectKind kind, int stacks) {
        return of(kind, 0, DamageSink.none(), new EffectDynamics.Stacks(stacks, 0));
    }

    /**
     * {@code stacks} more points of lost resilience, which wear off one at a time. Allowed up to a
     * hundred, the whole of resilience.
     */
    public static Effect scorched(int stacks) {
        return stackDebuff(EffectKind.SCORCHED, stacks).withStackCap(SICKENED_STACK_CAP);
    }

    /**
     * {@code stacks} more points of lost spirit, which wear off one at a time. Allowed up to a hundred,
     * the whole of spirit.
     */
    public static Effect sickened(int stacks) {
        return stackDebuff(EffectKind.SICKENED, stacks).withStackCap(SICKENED_STACK_CAP);
    }

    /** {@code stacks} more hits' worth of extra damage taken, on one shared clock. */
    public static Effect vulnerable(int stacks, int durationTicks, DamageSink sink) {
        return stacking(EffectKind.VULNERABLE, stacks, durationTicks, sink);
    }

    /** {@code stacks} more points of lost armor, five each, on one shared clock. */
    public static Effect sundered(int stacks, int durationTicks, DamageSink sink) {
        return stacking(EffectKind.SUNDERED, stacks, durationTicks, sink);
    }

    /** This stacking effect allowed up to {@code stackCap} stacks instead of its kind's cap. */
    public Effect withStackCap(int stackCap) {
        return this.withDynamics(this.stacksFamily().withStackCap(stackCap));
    }

    /** The most stacks it may reach: its own cap, else its kind's. */
    public int effectiveStackCap() {
        return this.stackCap() > 0 ? this.stackCap() : this.kind.maxStacks();
    }


    /**
     * The next hit from any attacker other than {@code origin} discharges it, dealing extra magic
     * through {@code sink}.
     */
    public static Effect charged(int durationTicks, AttackOrigin origin, DamageSink sink) {
        return new Effect(EffectKind.CHARGED, durationTicks, durationTicks, sink, origin, FLAG);
    }

    /** A hex of {@code kind} for {@code durationTicks}; what it pays out goes through {@code sink}. */
    public static Effect hex(EffectKind kind, int durationTicks, DamageSink sink) {
        if (kind.category() != EffectCategory.HEX) {
            throw new IllegalArgumentException(kind + " is not a hex");
        }
        return timed(kind, durationTicks, sink);
    }

    /**
     * The Blight hex: a slow that grows, from {@code start}, by {@code slowPerTick} with every tick it runs, never below
     * {@code floor}. What lengthens or shortens it as it is applied changes how far it grows.
     */
    public static Effect blight(int durationTicks, float slowPerTick, float start, float floor, DamageSink sink) {
        return of(EffectKind.BLIGHT, durationTicks, sink, new EffectDynamics.GrowingSlow(slowPerTick, start, floor));
    }

    /** Its abilities don't fire for {@code durationTicks}. */
    public static Effect silenced(int durationTicks, DamageSink sink) {
        return timed(EffectKind.SILENCED, durationTicks, sink);
    }

    /** Its speed is held to a share of its base, whatever raises it, for {@code durationTicks}. */
    public static Effect anchored(int durationTicks, DamageSink sink) {
        return timed(EffectKind.ANCHORED, durationTicks, sink);
    }

    /** {@code stacks} more steps of lost magic resist, on one shared clock. */
    public static Effect unraveled(int stacks, int durationTicks, DamageSink sink) {
        return stacking(EffectKind.UNRAVELED, stacks, durationTicks, sink);
    }

    /** Extra physical damage taken while it lasts. */
    public static Effect brittle(int durationTicks, DamageSink sink) {
        return timed(EffectKind.BRITTLE, durationTicks, sink);
    }

    /** {@code stacks} more Toll, up to {@code cap}; they wear off a stack at a time outside every field. */
    public static Effect toll(int stacks, int cap) {
        return stackDebuff(EffectKind.TOLL, stacks).withStackCap(cap);
    }

    /** Takes {@code strength} of speed away for {@code durationTicks}: the field's slow. */
    public static Effect dragged(float strength, int durationTicks, DamageSink sink) {
        return of(EffectKind.DRAGGED, durationTicks, sink, new EffectDynamics.Slow(strength));
    }

    /** Heals, shields and rewinds are worth nothing for {@code durationTicks}. */
    public static Effect deadZone(int durationTicks, DamageSink sink) {
        return timed(EffectKind.DEAD_ZONE, durationTicks, sink);
    }

    /**
     * Physical damage for every cell the enemy travels while it lasts, {@code damagePerCell} (in
     * damage units) each. It is carried in {@code damagePerTick}, which a bleed never deals as such.
     */
    public static Effect bleeding(int damagePerCell, int durationTicks, DamageSink sink) {
        return of(EffectKind.BLEEDING, durationTicks, sink, new EffectDynamics.Distance(Damage.physical(damagePerCell)));
    }

    /** Plating halved for {@code durationTicks}. */
    public static Effect cracked(int durationTicks, DamageSink sink) {
        return timed(EffectKind.CRACKED, durationTicks, sink);
    }

    /** Crack that takes more plating, for {@code durationTicks}. */
    public static Effect deeplyCracked(int durationTicks, DamageSink sink) {
        return timed(EffectKind.DEEP_CRACKED, durationTicks, sink);
    }

    /** Stuck in tar for {@code durationTicks}. */
    public static Effect tarred(int durationTicks, DamageSink sink) {
        return timed(EffectKind.TARRED, durationTicks, sink);
    }

    /** Crits find it more often and hit it harder while it lasts. */
    public static Effect exposed(int durationTicks, DamageSink sink) {
        return timed(EffectKind.EXPOSED, durationTicks, sink);
    }

    /** The next hit lands as a guaranteed crit and ends the mark; it ends by itself after {@code durationTicks}. */
    public static Effect marked(int durationTicks, DamageSink sink) {
        return timed(EffectKind.MARKED, durationTicks, sink);
    }

    /** Extra damage from every tower, and the preferred target of towers that pick one. */
    public static Effect priority(int durationTicks, DamageSink sink) {
        return timed(EffectKind.PRIORITY, durationTicks, sink);
    }

    static Effect stacking(EffectKind kind, int stacks, int durationTicks, DamageSink sink) {
        return of(kind, durationTicks, sink, new EffectDynamics.Stacks(stacks, 0));
    }

    private static Effect timed(EffectKind kind, int durationTicks, DamageSink sink) {
        return of(kind, durationTicks, sink, FLAG);
    }

    private static Effect of(EffectKind kind, int durationTicks, DamageSink sink, EffectDynamics dynamics) {
        return new Effect(kind, durationTicks, durationTicks, sink, AttackOrigin.none(), dynamics);
    }

    /**
     * Adds {@code healPerTick} to regeneration while it lasts. Not a negative {@code damagePerTick}: damage clamps
     * at zero, and a heal credits nobody. Heals from different sources ({@link #from}) add up.
     */
    public static Effect heal(int healPerTick, int durationTicks, DamageSink sink) {
        return of(EffectKind.HEAL, durationTicks, sink,
                new EffectDynamics.HealOverTime(List.of(new HealShare(AttackOrigin.none(), healPerTick, durationTicks))));
    }

    /** This effect as put on by {@code origin}: what tells one source's heal from another's. */
    public Effect from(AttackOrigin origin) {
        EffectDynamics keyed = switch (this.dynamics) {
            case EffectDynamics.HealOverTime heal -> heal.from(origin);
            case EffectDynamics.ShieldPool shield -> shield.from(origin);
            case EffectDynamics.FuelPool pool -> pool.from(origin);
            default -> this.dynamics;
        };
        return new Effect(this.kind, this.remainingTicks, this.authoredDurationTicks, this.sink, origin, keyed);
    }

    /** With {@code incoming}, a heal or a shield like this one, joined in: its shares refresh or add to these. */
    Effect joinedBy(Effect incoming) {
        EffectDynamics joined = switch (incoming.dynamics()) {
            case EffectDynamics.HealOverTime more -> this.healFamily().with(more);
            case EffectDynamics.ShieldPool more -> this.shieldPool().with(more);
            default -> throw incoming.wrongFamily("a heal or a shield");
        };
        return new Effect(this.kind, this.remainingTicks, Math.max(this.authoredDurationTicks,
                incoming.authoredDurationTicks()), this.sink, this.origin, joined);
    }

    /** This shield holding {@code shield}'s shares. */
    Effect withShield(EffectDynamics.ShieldPool shield) {
        return this.withDynamics(shield);
    }

    /** The share of speed a growing slow takes away now, from the time it has run; {@code 0f} for any other family. */
    public float grownSlow() {
        return switch (this.dynamics) {
            case EffectDynamics.GrowingSlow growing -> growing.strengthAfter(this.authoredDurationTicks - this.remainingTicks);
            default -> 0f;
        };
    }

    /** The share of speed this slow takes away at full strength; {@code 0f} for a kind that doesn't slow. */
    public float slowStrength() {
        return switch (this.dynamics) {
            case EffectDynamics.Slow slow -> slow.strength();
            case EffectDynamics.Stacks stacks -> this.kind.rule().slowPerStack() * stacks.stacks();
            default -> this.kind.rule().slowPerStack();
        };
    }

    /** The fuel a pool still holds, in damage units: what it will still deal. {@code 0f} for any other kind. */
    public float fuelLevel() {
        float total = 0f;
        for (FuelContribution contribution : this.fuel()) {
            total += contribution.fuel();
        }
        return total;
    }

    /** A pool's contributions, one per source; empty for any other family. */
    List<FuelContribution> fuel() {
        return switch (this.dynamics) {
            case EffectDynamics.FuelPool pool -> pool.fuel();
            default -> List.of();
        };
    }

    /** The ticks until a pool earns its next stack, one every {@link #STACK_INTERVAL_TICKS}. */
    public int stackClock() {
        return switch (this.dynamics) {
            case EffectDynamics.FuelPool pool -> pool.stackClock();
            default -> 0;
        };
    }

    /** The ice a freeze is made of; {@link IceKind#FROST} for any other family. */
    public IceKind ice() {
        return switch (this.dynamics) {
            case EffectDynamics.Encasing encasing -> encasing.ice();
            default -> IceKind.FROST;
        };
    }

    /** Whether an enemy immune to {@code immunities} keeps this effect off: a ruby freeze still holds. */
    public boolean isKeptOffBy(Set<EffectKind> immunities) {
        return immunities.contains(this.kind) && !this.ice().holdsTheImmune();
    }

    /** A stacking kind's stack count. */
    public int stacks() {
        return switch (this.dynamics) {
            case EffectDynamics.Stacks stacks -> stacks.stacks();
            default -> 0;
        };
    }

    /** The most stacks a stacking kind may reach, {@code 0} for its kind's own cap. */
    public int stackCap() {
        return switch (this.dynamics) {
            case EffectDynamics.Stacks stacks -> stacks.stackCap();
            default -> 0;
        };
    }

    /** What a shield's pool has left, in damage units. */
    public float shieldRemaining() {
        return switch (this.dynamics) {
            case EffectDynamics.ShieldPool shield -> shield.remaining();
            default -> 0f;
        };
    }

    public float shieldMax() {
        return switch (this.dynamics) {
            case EffectDynamics.ShieldPool shield -> shield.max();
            default -> 0f;
        };
    }

    /** Each source's heal; empty for any other family. */
    public List<HealShare> healShares() {
        return switch (this.dynamics) {
            case EffectDynamics.HealOverTime heal -> heal.shares();
            default -> List.of();
        };
    }

    /** Each source's shield; empty for any other family. */
    public List<ShieldShare> shieldShares() {
        return switch (this.dynamics) {
            case EffectDynamics.ShieldPool shield -> shield.shares();
            default -> List.of();
        };
    }

    /** What every source's heal adds to regeneration together, in damage units a tick. */
    public int healPerTick() {
        return switch (this.dynamics) {
            case EffectDynamics.HealOverTime heal -> heal.perTick();
            default -> 0;
        };
    }

    /** A bleed's damage a cell; none for any other family. */
    public Damage damagePerTick() {
        return switch (this.dynamics) {
            case EffectDynamics.Distance distance -> distance.perCell();
            default -> Damage.none();
        };
    }

    /** Narrows a shield to one damage type; the other passes through. */
    public Effect withShieldRestrictedTo(DamageType type) {
        return this.withDynamics(this.shieldPool().restrictedTo(type));
    }

    /** Both the remaining and the authored duration times {@code factor}, rounded and never below a tick. */
    public Effect withDurationScaledBy(float factor) {
        if (this.isLasting() && factor > 0f) {
            return this;
        }
        EffectDynamics scaled = this.retimed(ticks -> Math.round(ticks * factor));
        return new Effect(this.kind, Math.max(1, Math.round(this.remainingTicks * factor)),
                Math.max(1, Math.round(this.authoredDurationTicks * factor)), this.sink, this.origin, scaled);
    }

    /**
     * Whether it lasts for good rather than for a time: one authored so, or a shield one of whose shares holds until
     * broken.
     */
    public boolean isLasting() {
        return this.remainingTicks == LASTING_TICKS;
    }

    /**
     * This effect with {@code ticks} gone from the time it has left, from each share's for a heal or a shield; empty
     * once none is left.
     */
    public Optional<Effect> afterTicks(int ticks) {
        if (this.isLasting() && this.shieldShares().isEmpty()) {
            return Optional.of(this);
        }
        Effect after = new Effect(this.kind, this.remainingTicks - ticks, this.authoredDurationTicks, this.sink,
                this.origin, this.retimed(left -> left - ticks));
        return after.remainingTicks() > 0 ? Optional.of(after) : Optional.empty();
    }

    /** This effect on one clock with {@code remainingTicks} left; a heal or a shield keeps a clock per share instead. */
    Effect withRemainingTicks(int remainingTicks) {
        return switch (this.dynamics) {
            case EffectDynamics.HealOverTime heal -> throw this.wrongFamily("on one clock");
            case EffectDynamics.ShieldPool shield -> throw this.wrongFamily("on one clock");
            default -> new Effect(this.kind, remainingTicks, this.authoredDurationTicks, this.sink, this.origin,
                    this.dynamics);
        };
    }

    /** The shares' clocks through {@code time}, for a heal or a shield; any other family keeps its dynamics. */
    private EffectDynamics retimed(IntUnaryOperator time) {
        return switch (this.dynamics) {
            case EffectDynamics.HealOverTime heal -> heal.retimed(time);
            case EffectDynamics.ShieldPool shield -> shield.retimed(time);
            default -> this.dynamics;
        };
    }

    /** Carries a pool forward after it burns or is fed. */
    Effect withFuel(List<FuelContribution> fuel) {
        return this.withDynamics(this.poolFamily().withFuel(fuel));
    }

    /**
     * This pool with {@code factor} of its fuel: what tenacity makes of fuel as it comes in, so the time it burns and
     * what it deals both shrink.
     */
    public Effect withFuelScaledBy(float factor) {
        return this.withDynamics(this.poolFamily().scaledBy(factor));
    }

    /** The type of damage a pool deals. */
    DamageType poolType() {
        return this.poolFamily().type();
    }

    /** The same effect with a new stack count and a fresh clock of {@code remainingTicks}. */
    Effect withStacks(int stacks, int remainingTicks) {
        return new Effect(this.kind, remainingTicks, this.authoredDurationTicks, this.sink, this.origin,
                this.stacksFamily().withStacks(stacks));
    }

    /** The same pool with its clock to the next stack set to {@code stackClock}. */
    Effect withStackClock(int stackClock) {
        return this.withDynamics(this.poolFamily().withStackClock(stackClock));
    }

    private Effect withDynamics(EffectDynamics dynamics) {
        return new Effect(this.kind, this.remainingTicks, this.authoredDurationTicks, this.sink, this.origin, dynamics);
    }

    private EffectDynamics.Stacks stacksFamily() {
        return switch (this.dynamics) {
            case EffectDynamics.Stacks stacks -> stacks;
            default -> throw this.wrongFamily("a stack count");
        };
    }

    private EffectDynamics.FuelPool poolFamily() {
        return switch (this.dynamics) {
            case EffectDynamics.FuelPool pool -> pool;
            default -> throw this.wrongFamily("a pool");
        };
    }

    private EffectDynamics.HealOverTime healFamily() {
        return switch (this.dynamics) {
            case EffectDynamics.HealOverTime heal -> heal;
            default -> throw this.wrongFamily("a heal");
        };
    }

    EffectDynamics.ShieldPool shieldPool() {
        return switch (this.dynamics) {
            case EffectDynamics.ShieldPool shield -> shield;
            default -> throw this.wrongFamily("a shield");
        };
    }

    private IllegalStateException wrongFamily(String expected) {
        return new IllegalStateException(this.kind + " is not " + expected);
    }
}
