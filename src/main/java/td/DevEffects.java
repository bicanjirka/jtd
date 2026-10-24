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

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * What the dev panel can put on a unit: every enemy effect, each built from its own {@link DevField fields}, or the
 * reason the panel can't make it work; and the tower buffs it offers. Nothing it makes is credited to a tower: its
 * damage goes through the sink it is given.
 */
public final class DevEffects {

    private static final DevField SECONDS = DevField.seconds();
    private static final DevField SHOTS = DevField.shots();
    private static final DevField SLOW = new DevField("Slow %", 40f, 1f, 80f);
    private static final DevField DAMAGE_PER_SECOND = new DevField("Dmg/s", 20f, 0.1f, 100_000f);
    private static final DevField SHIELD_POINTS = new DevField("Points", 50f, 1f, 100_000f);
    private static final DevField HEAL_PER_SECOND = new DevField("Heal/s", 10f, 0.1f, 100_000f);
    private static final DevField DAMAGE_PER_CELL = new DevField("Dmg/cell", 5f, 0.1f, 100_000f);
    private static final DevField ATTACK_SPEED = new DevField("Speed %", 50f, 1f, 900f);
    private static final DevField DAMAGE_TIMES = new DevField("Damage ×", 3f, 1f, 100f);
    /** A stack field starts at the cap, or at this many for a kind that holds more. */
    private static final int MOST_STACKS_TO_START = 10;

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

    /** What the panel asks for before it applies {@code kind}, in order; none for a kind it can't make work. */
    public static List<DevField> enemyFields(EffectKind kind) {
        return switch (offer(kind)) {
            case Builds builds -> builds.fields();
            case Refuses refuses -> List.of();
        };
    }

    /**
     * What applying {@code kind} puts on the enemy, in order: one effect, or more for a kind that brings another
     * with it; none for a kind {@link #whyUnusable unusable} here. {@code values} fill its {@link #enemyFields
     * fields}, in order. Its damage goes through {@code sink}, and it is put on by {@code origin}.
     */
    public static List<Effect> enemyEffects(EffectKind kind, List<Float> values, DamageSink sink, AttackOrigin origin) {
        return switch (offer(kind)) {
            case Builds builds -> builds.builder().apply(new Inputs(values, sink, origin));
            case Refuses refuses -> List.of();
        };
    }

    private static Offer offer(EffectKind kind) {
        return switch (kind) {
            case CHILL -> one(List.of(SECONDS, SLOW), in -> Effect.chill(in.value(1) / 100f, in.ticks(0), in.sink()));
            case BURN -> one(List.of(SECONDS, DAMAGE_PER_SECOND), in -> Effect.burn(in.magicPerTick(1), in.ticks(0), in.sink()));
            case POISON -> one(List.of(SECONDS, DAMAGE_PER_SECOND), in -> Effect.poison(in.magicPerTick(1), in.ticks(0), in.sink()));
            case SOULFIRE -> one(List.of(SECONDS, DAMAGE_PER_SECOND),
                    in -> Effect.soulfire(in.magicPerTick(1), in.ticks(0), in.sink()));
            case BLEEDING -> one(List.of(SECONDS, DAMAGE_PER_CELL),
                    in -> Effect.bleeding(DamageUnits.ofPoints(in.value(1)), in.ticks(0), in.sink()));
            case SHIELD -> one(List.of(SECONDS, SHIELD_POINTS), in -> Effect.shield(DamageUnits.ofPoints(in.value(1)), in.ticks(0), in.sink()));
            case HEAL -> one(List.of(SECONDS, HEAL_PER_SECOND),
                    in -> Effect.heal(in.unitsPerTick(1), in.ticks(0), in.sink()));
            case FREEZE -> timed(Effect::freeze);
            case DAZED -> timed(Effect::dazed);
            case INVISIBLE -> timed(Effect::invisible);
            case SPRINTING -> timed(Effect::sprinting);
            case HASTENED -> timed(Effect::hastened);
            case REVEALED -> timed(Effect::revealed);
            case UNVEILED -> timed(Effect::unveiled);
            case SILENCED -> timed(Effect::silenced);
            case ANCHORED -> timed(Effect::anchored);
            case UNDERTOW -> timed(Effect::undertow);
            case BRITTLE -> timed(Effect::brittle);
            case CRACKED -> timed(Effect::cracked);
            case TARRED -> timed(Effect::tarred);
            case DEAD_ZONE -> timed(Effect::deadZone);
            case FALLOUT -> timed(Effect::fallout);
            case EXPOSED -> timed(Effect::exposed);
            case MARKED -> timed(Effect::marked);
            case PRIORITY -> timed(Effect::priority);
            case CHARGED -> one(List.of(SECONDS), in -> Effect.charged(in.ticks(0), in.origin(), in.sink()));
            case VULNERABLE -> stacking(kind, Effect::vulnerable);
            case SUNDERED -> stacking(kind, Effect::sundered);
            case RESONATING -> stacking(kind, Effect::resonating);
            case UNRAVELED -> stacking(kind, Effect::unraveled);
            case CONDEMNED -> stacking(kind, Effect::condemned);
            case SATURATED -> stacking(kind, Effect::saturated);
            case TOLL -> stacking(kind, Effect::toll);
            case CORRODED -> stacking(kind, Effect::corroded);
            case DRAGGED -> stacking(kind, Effect::dragged);
            case FRAIL -> stacking(kind, Effect::frail);
            case FRACTURED -> one(List.of(stacks(kind)), in -> Effect.fractured(in.whole(0), in.sink()));
            case SCORCHED -> one(List.of(stacks(kind)), in -> Effect.scorched(in.whole(0)));
            case SICKENED -> one(List.of(stacks(kind)), in -> Effect.sickened(in.whole(0)));
            case RIME, ASH, INVERSION, DOOM, CONTAGION, SYMPATHY, RECKONING -> timed((ticks, sink) -> Effect.hex(kind, ticks, sink));
            case BLIGHT -> new Builds(List.of(SECONDS, DAMAGE_PER_SECOND), in -> List.of(Effect.hex(kind, in.ticks(0), in.sink()),
                    Effect.poison(in.magicPerTick(1), in.ticks(0), in.sink())));
            case STILLED -> new Refuses("needs an Obelisk field");
        };
    }

    /** The tower buffs the panel offers. */
    public static List<TowerTerm> towerBuffs() {
        return List.of(TowerTerm.HASTE, TowerTerm.PATIENCE, TowerTerm.CHARGED_SHOT);
    }

    /** What the panel asks for before it grants {@code term}, in order. */
    public static List<DevField> buffFields(TowerTerm term) {
        return buff(term).fields();
    }

    /** {@code term} built from {@code values}, which fill its {@link #buffFields fields} in order. */
    public static TowerStatus towerBuff(TowerTerm term, List<Float> values) {
        return buff(term).builder().apply(new Inputs(values, DamageSink.none(), AttackOrigin.none()));
    }

    private static BuffRecipe buff(TowerTerm term) {
        return switch (term) {
            case HASTE -> new BuffRecipe(List.of(SECONDS, ATTACK_SPEED),
                    in -> TowerStatus.timed(term, TowerModifiers.attackSpeed(in.value(1) / 100f), in.ticks(0)));
            case PATIENCE -> new BuffRecipe(List.of(SHOTS), in -> TowerStatus.nextShots(term, in.whole(0),
                    StatModifiers.<TowerStat>none()).withCapability(TowerCapability.GUARANTEED_CRIT));
            case CHARGED_SHOT -> new BuffRecipe(List.of(SHOTS, DAMAGE_TIMES), in -> TowerStatus.nextShots(term, in.whole(0),
                    TowerModifiers.damageTimes(in.value(1)).plus(TowerModifiers.armorPenetration(1f))
                            .plus(TowerModifiers.flatPenetration(1f))));
            default -> throw new IllegalArgumentException(term + " is no buff the panel grants");
        };
    }

    private static Offer one(List<DevField> fields, Function<Inputs, Effect> builder) {
        return new Builds(fields, in -> List.of(builder.apply(in)));
    }

    private static Offer timed(TimedMaker maker) {
        return one(List.of(SECONDS), in -> maker.make(in.ticks(0), in.sink()));
    }

    private static Offer stacking(EffectKind kind, StackingMaker maker) {
        return one(List.of(SECONDS, stacks(kind)), in -> maker.make(in.whole(1), in.ticks(0), in.sink()));
    }

    private static DevField stacks(EffectKind kind) {
        return new DevField("Stacks", Math.min(kind.maxStacks(), MOST_STACKS_TO_START), 1f, kind.maxStacks());
    }

    /** What the panel was given for one application: its fields' values, in order. */
    private record Inputs(List<Float> values, DamageSink sink, AttackOrigin origin) {

        float value(int field) {
            return this.values.get(field);
        }

        int whole(int field) {
            return Math.round(this.value(field));
        }

        int ticks(int field) {
            return Math.max(1, Math.round(this.value(field) * TickRate.TICKS_PER_SECOND));
        }

        /** A field in points a second, as units each tick. */
        int unitsPerTick(int field) {
            return DamageUnits.ofPoints(this.value(field) / TickRate.TICKS_PER_SECOND);
        }

        Damage magicPerTick(int field) {
            return Damage.magic(this.unitsPerTick(field));
        }
    }

    private sealed interface Offer permits Builds, Refuses {
    }

    private record Builds(List<DevField> fields, Function<Inputs, List<Effect>> builder) implements Offer {
    }

    private record Refuses(String reason) implements Offer {
    }

    private record BuffRecipe(List<DevField> fields, Function<Inputs, TowerStatus> builder) {
    }

    private interface TimedMaker {
        Effect make(int ticks, DamageSink sink);
    }

    private interface StackingMaker {
        Effect make(int stacks, int ticks, DamageSink sink);
    }
}
