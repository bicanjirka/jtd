package td;

import td.damage.AttackOrigin;
import td.damage.Damage;
import td.damage.DamageUnits;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.stat.StatModifiers;
import td.tower.TowerCapability;
import td.tower.TowerModifiers;
import td.tower.TowerStat;
import td.tower.TowerStatus;
import td.tower.TowerTerm;
import td.util.TickRate;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * What the dev panel can put on a unit: every enemy effect, each built from a length in seconds and one power number
 * whose meaning is the kind's own (stacks, percent, points), or the reason the panel can't make it work; and the tower
 * buffs it offers. Nothing it makes is credited to a tower: its damage goes through the sink it is given.
 */
public final class DevEffects {

    private static final float MOST_CHILL = 0.8f;
    private static final String NEEDS_A_HEXER = "needs a Hexer";

    private static final Map<TowerTerm, BuffBuilder> TOWER = new EnumMap<>(TowerTerm.class);

    static {
        TOWER.put(TowerTerm.HASTE, (ticks, power) -> TowerStatus.timed(TowerTerm.HASTE,
                TowerModifiers.attackSpeed(power / 100f), ticks));
        TOWER.put(TowerTerm.PATIENCE, (ticks, power) -> TowerStatus.nextShots(TowerTerm.PATIENCE, 1,
                StatModifiers.<TowerStat>none()).withCapability(TowerCapability.GUARANTEED_CRIT));
        TOWER.put(TowerTerm.CHARGED_SHOT, (ticks, power) -> TowerStatus.nextShots(TowerTerm.CHARGED_SHOT, 1,
                TowerModifiers.damageTimes(Math.max(1, power))));
    }

    private DevEffects() {
    }

    /** Every enemy effect, the ones the panel can't make work among them. */
    public static List<EffectKind> enemyKinds() {
        return List.of(EffectKind.values());
    }

    /** Why the panel can't make {@code kind} work on its own; empty when it can. */
    public static Optional<String> whyUnusable(EffectKind kind) {
        return switch (offer(kind)) {
            case Builds builds -> Optional.empty();
            case Refuses refuses -> Optional.of(refuses.reason());
        };
    }

    /**
     * What applying {@code kind} puts on the enemy, in order: one effect, or more for a kind that brings another
     * with it; none for a kind {@link #whyUnusable unusable} here. Its damage goes through {@code sink}, and it is
     * put on by {@code origin}.
     */
    public static List<Effect> enemyEffects(EffectKind kind, float seconds, int power, DamageSink sink,
            AttackOrigin origin) {
        return switch (offer(kind)) {
            case Builds builds -> builds.builder().apply(new Inputs(ticks(seconds), power, sink, origin));
            case Refuses refuses -> List.of();
        };
    }

    private static Offer offer(EffectKind kind) {
        return switch (kind) {
            case CHILL -> one(in -> Effect.chill(Math.clamp(in.power() / 100f, 0f, MOST_CHILL), in.ticks(), in.sink()));
            case BURN -> one(in -> Effect.burn(pool(in.power()), in.ticks(), in.sink()));
            case POISON -> one(in -> Effect.poison(pool(in.power()), in.ticks(), in.sink()));
            case SOULFIRE -> one(in -> Effect.soulfire(pool(in.power()), in.ticks(), in.sink()));
            case BLEEDING -> one(in -> Effect.bleeding(DamageUnits.ofPoints(in.power()), in.ticks(), in.sink()));
            case SHIELD -> one(in -> Effect.shield(DamageUnits.ofPoints(in.power()), in.ticks(), in.sink()));
            case HEAL -> one(in -> Effect.heal(DamageUnits.ofPoints(in.power()), in.ticks(), in.sink()));
            case FREEZE -> one(in -> Effect.freeze(in.ticks(), in.sink()));
            case DAZED -> one(in -> Effect.dazed(in.ticks(), in.sink()));
            case INVISIBLE -> one(in -> Effect.invisible(in.ticks(), in.sink()));
            case SPRINTING -> one(in -> Effect.sprinting(in.ticks(), in.sink()));
            case HASTENED -> one(in -> Effect.hastened(in.ticks(), in.sink()));
            case REVEALED -> one(in -> Effect.revealed(in.ticks(), in.sink()));
            case UNVEILED -> one(in -> Effect.unveiled(in.ticks(), in.sink()));
            case SILENCED -> one(in -> Effect.silenced(in.ticks(), in.sink()));
            case ANCHORED -> one(in -> Effect.anchored(in.ticks(), in.sink()));
            case UNDERTOW -> one(in -> Effect.undertow(in.ticks(), in.sink()));
            case BRITTLE -> one(in -> Effect.brittle(in.ticks(), in.sink()));
            case CRACKED -> one(in -> Effect.cracked(in.ticks(), in.sink()));
            case TARRED -> one(in -> Effect.tarred(in.ticks(), in.sink()));
            case DEAD_ZONE -> one(in -> Effect.deadZone(in.ticks(), in.sink()));
            case FALLOUT -> one(in -> Effect.fallout(in.ticks(), in.sink()));
            case EXPOSED -> one(in -> Effect.exposed(in.ticks(), in.sink()));
            case MARKED -> one(in -> Effect.marked(in.ticks(), in.sink()));
            case PRIORITY -> one(in -> Effect.priority(in.ticks(), in.sink()));
            case CHARGED -> one(in -> Effect.charged(in.ticks(), in.origin(), in.sink()));
            case VULNERABLE -> one(in -> Effect.vulnerable(in.power(), in.ticks(), in.sink()));
            case SUNDERED -> one(in -> Effect.sundered(in.power(), in.ticks(), in.sink()));
            case RESONATING -> one(in -> Effect.resonating(in.power(), in.ticks(), in.sink()));
            case UNRAVELED -> one(in -> Effect.unraveled(in.power(), in.ticks(), in.sink()));
            case CONDEMNED -> one(in -> Effect.condemned(in.power(), in.ticks(), in.sink()));
            case SATURATED -> one(in -> Effect.saturated(in.power(), in.ticks(), in.sink()));
            case TOLL -> one(in -> Effect.toll(in.power(), in.ticks(), in.sink()));
            case CORRODED -> one(in -> Effect.corroded(in.power(), in.ticks(), in.sink()));
            case DRAGGED -> one(in -> Effect.dragged(in.power(), in.ticks(), in.sink()));
            case FRAIL -> one(in -> Effect.frail(in.power(), in.ticks(), in.sink()));
            case FRACTURED -> one(in -> Effect.fractured(in.power(), in.sink()));
            case SCORCHED -> one(in -> Effect.scorched(in.power()));
            case SICKENED -> one(in -> Effect.sickened(in.power()));
            case RIME, ASH, INVERSION -> one(in -> Effect.hex(kind, in.ticks(), in.sink()));
            case BLIGHT -> new Builds(in -> List.of(Effect.hex(kind, in.ticks(), in.sink()),
                    Effect.poison(pool(in.power()), in.ticks(), in.sink())));
            case DOOM, CONTAGION, SYMPATHY, RECKONING -> new Refuses(NEEDS_A_HEXER);
            case STILLED -> new Refuses("needs an Obelisk field");
        };
    }

    /** The tower buffs the panel offers, in the order it cycles them. */
    public static List<TowerTerm> towerBuffs() {
        return List.copyOf(TOWER.keySet());
    }

    public static TowerStatus towerBuff(TowerTerm term, float seconds, int power) {
        return TOWER.get(term).build(ticks(seconds), power);
    }

    private static Offer one(Function<Inputs, Effect> builder) {
        return new Builds(in -> List.of(builder.apply(in)));
    }

    private static int ticks(float seconds) {
        return Math.max(1, Math.round(seconds * TickRate.TICKS_PER_SECOND));
    }

    private static Damage pool(int power) {
        return Damage.magic(DamageUnits.ofPoints(power));
    }

    /** What the panel was given for one application. */
    private record Inputs(int ticks, int power, DamageSink sink, AttackOrigin origin) {
    }

    private sealed interface Offer permits Builds, Refuses {
    }

    private record Builds(Function<Inputs, List<Effect>> builder) implements Offer {
    }

    private record Refuses(String reason) implements Offer {
    }

    private interface BuffBuilder {
        TowerStatus build(int ticks, int power);
    }
}
