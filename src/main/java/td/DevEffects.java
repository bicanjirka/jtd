package td;

import td.damage.AttackOrigin;
import td.damage.Damage;
import td.damage.DamageUnits;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.stat.DisruptionAura;
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
 * What the dev panel can put on a unit: every enemy effect and every tower term, each built from its own
 * {@link DevField fields}, or the reason the panel can't make it work. Nothing it makes is credited to a tower: an
 * effect's damage goes through the sink it is given.
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
    private static final DevField CRIT_DAMAGE = new DevField("Crit +%", 150f, 1f, 1000f);
    private static final DevField MAGIC_PIERCE = new DevField("Pierce %", 25f, 1f, 100f);
    private static final DevField ATTACK_SPEED_LOST = new DevField("Slow %", 30f, 1f, 75f);
    private static final DevField RANGE_LOST = new DevField("Range -", 1f, 0f, 20f);
    /** A stack field starts at the cap, or at this many for a kind that holds more. */
    private static final int MOST_STACKS_TO_START = 10;
    /** A dev jam reaches only the tower it is held on: a quarter cell around its centre. */
    private static final float JAM_REACH_CELLS = 0.25f;
    private static final String KEPT_BY_THE_TOWER = "the tower keeps it itself";
    private static final String FROM_AN_UPGRADE = "comes with an upgrade";

    private DevEffects() {
    }

    /** Every enemy effect, the ones the panel can't make work among them. */
    public static List<EffectKind> enemyKinds() {
        return List.of(EffectKind.values());
    }

    /** Why the panel can't make {@code kind} work on its own; empty when it can. */
    public static Optional<String> whyUnusable(EffectKind kind) {
        return offer(kind).refusal();
    }

    /** What the panel asks for before it applies {@code kind}, in order; none for a kind it can't make work. */
    public static List<DevField> enemyFields(EffectKind kind) {
        return offer(kind).fields();
    }

    /**
     * What applying {@code kind} puts on the enemy, in order: one effect, or more for a kind that brings another
     * with it; none for a kind {@link #whyUnusable unusable} here. {@code values} fill its {@link #enemyFields
     * fields}, in order. Its damage goes through {@code sink}, and it is put on by {@code origin}.
     */
    public static List<Effect> enemyEffects(EffectKind kind, List<Float> values, DamageSink sink, AttackOrigin origin) {
        return offer(kind).build(new Inputs(values, sink, origin, 0)).orElse(List.of());
    }

    /** Every tower term, the ones the panel can't grant among them. */
    public static List<TowerTerm> towerTerms() {
        return List.of(TowerTerm.values());
    }

    /** Why the panel can't grant {@code term}; empty when it can. */
    public static Optional<String> whyUngrantable(TowerTerm term) {
        return grant(term).refusal();
    }

    /** What the panel asks for before it grants {@code term}, in order; none for a term it can't grant. */
    public static List<DevField> towerFields(TowerTerm term) {
        return grant(term).fields();
    }

    /**
     * What granting {@code term} does to a tower on a board {@code scale} pixels a cell, from {@code values} that fill
     * its {@link #towerFields fields} in order; empty for a term the panel can't grant.
     */
    public static Optional<TowerGrant> towerGrant(TowerTerm term, List<Float> values, int scale) {
        return grant(term).build(new Inputs(values, DamageSink.none(), AttackOrigin.none(), scale));
    }

    private static Offer<List<Effect>> offer(EffectKind kind) {
        return switch (kind) {
            case CHILL -> one(List.of(SECONDS, SLOW), in -> Effect.chill(in.value(1) / 100f, in.ticks(0), in.sink()));
            case BURN -> one(List.of(SECONDS, DAMAGE_PER_SECOND), in -> Effect.burn(in.magicPerTick(1), in.ticks(0), in.sink()));
            case POISON -> one(List.of(SECONDS, DAMAGE_PER_SECOND), in -> Effect.poison(in.magicPerTick(1), in.ticks(0), in.sink()));
            case SOULFIRE -> one(List.of(SECONDS, DAMAGE_PER_SECOND),
                    in -> Effect.soulfire(in.magicPerTick(1), in.ticks(0), in.sink()));
            case BLEEDING -> one(List.of(SECONDS, DAMAGE_PER_CELL),
                    in -> Effect.bleeding(DamageUnits.ofPoints(in.value(1)), in.ticks(0), in.sink()));
            case SHIELD -> one(List.of(SECONDS, SHIELD_POINTS),
                    in -> Effect.shield(DamageUnits.ofPoints(in.value(1)), in.ticks(0), in.sink()));
            case HEAL -> one(List.of(SECONDS, HEAL_PER_SECOND), in -> Effect.heal(in.unitsPerTick(1), in.ticks(0), in.sink()));
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
            case BLIGHT -> new Builds<>(List.of(SECONDS, DAMAGE_PER_SECOND), in -> List.of(Effect.hex(kind, in.ticks(0), in.sink()),
                    Effect.poison(in.magicPerTick(1), in.ticks(0), in.sink())));
            case STILLED -> new Refuses<>("needs an Obelisk field");
        };
    }

    private static Offer<TowerGrant> grant(TowerTerm term) {
        return switch (term) {
            case HASTE, RALLY -> status(List.of(SECONDS, ATTACK_SPEED),
                    in -> TowerStatus.timed(term, TowerModifiers.attackSpeed(in.value(1) / 100f), in.ticks(0)));
            case FRENZY -> status(List.of(SHOTS, ATTACK_SPEED),
                    in -> TowerStatus.nextShots(term, in.whole(0), TowerModifiers.attackSpeed(in.value(1) / 100f)));
            case PATIENCE -> status(List.of(SHOTS), in -> TowerStatus.nextShots(term, in.whole(0),
                    StatModifiers.<TowerStat>none()).withCapability(TowerCapability.GUARANTEED_CRIT));
            case CHARGED_SHOT -> status(List.of(SHOTS, DAMAGE_TIMES), in -> TowerStatus.nextShots(term, in.whole(0),
                    TowerModifiers.damageTimes(in.value(1)).plus(TowerModifiers.armorPenetration(1f))
                            .plus(TowerModifiers.flatPenetration(1f))));
            case DEADEYE_ROUND -> status(List.of(SHOTS, CRIT_DAMAGE), in -> TowerStatus.nextShots(term, in.whole(0),
                    TowerModifiers.critMultiplier(in.value(1) / 100f)).withCapability(TowerCapability.GUARANTEED_CRIT));
            case SILVER_ROUND -> status(List.of(SHOTS, MAGIC_PIERCE), in -> TowerStatus.nextShots(term, in.whole(0),
                    TowerModifiers.magicPenetration(in.value(1) / 100f)).withCapability(TowerCapability.MAGIC_DAMAGE));
            case JAMMED -> new Builds<>(List.of(SECONDS, ATTACK_SPEED_LOST, RANGE_LOST), in -> new TowerGrant.Jam(in.ticks(0),
                    new DisruptionAura(JAM_REACH_CELLS * in.scale(), in.value(1) / 100f, in.value(2))));
            case STEADY_AIM, CRIT_STREAK, FOCUS, BRACKETING, NEST, EVENT_HORIZON, INTERFERENCE, STOKE -> new Refuses<>(KEPT_BY_THE_TOWER);
            case NAPALM_SHELL, TAR_SHELL, CRYO_SHELL, NUKE_SHELL, CRYO_MISSILE, ARCANE_MISSILE, EMP_MISSILE, TRACER_MISSILE,
                    PING, SHATTER, ARC, HEX, SHRAPNEL, BOMBLET -> new Refuses<>(FROM_AN_UPGRADE);
            case AURA, KINSHIP -> new Refuses<>("comes from a Beacon");
            case LEY_LINE -> new Refuses<>("comes from two Obelisks");
        };
    }

    private static Offer<List<Effect>> one(List<DevField> fields, Function<Inputs, Effect> builder) {
        return new Builds<>(fields, in -> List.of(builder.apply(in)));
    }

    private static Offer<List<Effect>> timed(TimedMaker maker) {
        return one(List.of(SECONDS), in -> maker.make(in.ticks(0), in.sink()));
    }

    private static Offer<List<Effect>> stacking(EffectKind kind, StackingMaker maker) {
        return one(List.of(SECONDS, stacks(kind)), in -> maker.make(in.whole(1), in.ticks(0), in.sink()));
    }

    private static Offer<TowerGrant> status(List<DevField> fields, Function<Inputs, TowerStatus> builder) {
        return new Builds<>(fields, in -> new TowerGrant.Status(builder.apply(in)));
    }

    private static DevField stacks(EffectKind kind) {
        return new DevField("Stacks", Math.min(kind.maxStacks(), MOST_STACKS_TO_START), 1f, kind.maxStacks());
    }

    /** What granting a tower term does: a status the tower holds, or a jam the panel holds on it. */
    public sealed interface TowerGrant {

        /** A status the tower receives, as another tower would give it. */
        record Status(TowerStatus status) implements TowerGrant {
        }

        /** A disruption the panel lays on the tower every tick for {@code ticks}, as a nearby enemy would. */
        record Jam(int ticks, DisruptionAura aura) implements TowerGrant {
        }
    }

    /** What the panel was given for one application: its fields' values, in order, and the board's scale. */
    private record Inputs(List<Float> values, DamageSink sink, AttackOrigin origin, int scale) {

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

    /** One entry: what it asks for and how it is built, or why the panel can't build it. */
    private sealed interface Offer<R> permits Builds, Refuses {

        Optional<String> refusal();

        List<DevField> fields();

        Optional<R> build(Inputs inputs);
    }

    private record Builds<R>(List<DevField> fields, Function<Inputs, R> builder) implements Offer<R> {

        @Override
        public Optional<String> refusal() {
            return Optional.empty();
        }

        @Override
        public Optional<R> build(Inputs inputs) {
            return Optional.of(this.builder.apply(inputs));
        }
    }

    private record Refuses<R>(String reason) implements Offer<R> {

        @Override
        public Optional<String> refusal() {
            return Optional.of(this.reason);
        }

        @Override
        public List<DevField> fields() {
            return List.of();
        }

        @Override
        public Optional<R> build(Inputs inputs) {
            return Optional.empty();
        }
    }

    private interface TimedMaker {
        Effect make(int ticks, DamageSink sink);
    }

    private interface StackingMaker {
        Effect make(int stacks, int ticks, DamageSink sink);
    }
}
