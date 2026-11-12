package td.balance;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageUnits;
import td.effect.DamageSink;
import td.effect.Effect;
import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;
import td.stat.DisruptionPenalty;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.util.TickRate;
import td.wave.Vec2;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * A defence made of no tower: every tick it lands the hits and effects of a {@link Loadout} on the enemies of the
 * board through the calls a tower's hit makes, so armor, plating, dodging, hiding, shields, heals and immunities all
 * answer as they do in play. It reaches the whole board, so how long an enemy is under it is how long it walks.
 * <p>
 * It stands for towers spread evenly over the cells a defence would be built on: what enemies do to the towers near
 * them (a jam, a bellow, a blinding field) is sampled on each of those cells every tick, and the rig lands the share
 * of its loadout that towers standing there would still land. Installed as a playtest hook.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
final class ProbeRig implements IntConsumer {

    /** A single heavy hit, as a tower that picks one target lands. */
    static final float HEAVY_HIT_POINTS = 32f;
    /** One enemy's share of a blast: small, so a fixed loss per hit bites. */
    static final float BLAST_HIT_POINTS = 14f;
    private static final float BLAST_RADIUS_CELLS = 1.2f;
    private static final float SHOT_CRIT_CHANCE = 0.2f;
    private static final float SHOT_CRIT_MULTIPLIER = 2f;
    private static final int BURN_INTERVAL_TICKS = 20;
    private static final int BURN_TICKS = 60;
    private static final float CHILL_STRENGTH = 0.3f;
    private static final int CHILL_INTERVAL_TICKS = 10;
    private static final int CHILL_TICKS = 12;
    private static final int FREEZE_INTERVAL_TICKS = 100;
    private static final int FREEZE_TICKS = 20;
    private static final int REVEAL_TICKS = 2;
    /** No probe fires more often than this in a tick, so an immune enemy cannot hang a trial. */
    private static final int MOST_HITS_PER_TICK = 400;
    private static final AttackProfile SHOT = AttackProfile.critChance(SHOT_CRIT_CHANCE)
            .withCritMultiplier(SHOT_CRIT_MULTIPLIER).asAimed();
    private static final AttackProfile BOLT = AttackProfile.none().asAimed();
    private static final AttackProfile BLAST = AttackProfile.none();
    private static final AttackProfile BURNING = AttackProfile.none().asPeriodic();

    /** The reach a lost cell of range is taken from. */
    private static final float REACH_CELLS = 3.5f;

    private final GameWorld world;
    private final EnemyRegistry enemies;
    private final Loadout loadout;
    private final List<Vec2> emplacements;
    private final double blastRadius;
    private final Map<PowerProbe, Double> owed = new EnumMap<>(PowerProbe.class);
    private double burnStrength;

    /**
     * @param emplacements the centres of the cells its towers would stand on; with none, nothing weakens it
     */
    ProbeRig(GameWorld world, Loadout loadout, List<Vec2> emplacements) {
        this.world = world;
        this.enemies = world.enemies();
        this.loadout = loadout;
        this.emplacements = List.copyOf(emplacements);
        this.blastRadius = BLAST_RADIUS_CELLS * world.getBoard().scale();
    }

    /** The share of its loadout the towers it stands for still land this tick. */
    private double strength() {
        if (this.emplacements.isEmpty()) {
            return 1.0;
        }
        double sum = 0;
        for (Vec2 at : this.emplacements) {
            DisruptionPenalty penalty = this.world.disruptions().penaltyAt(at.x(), at.y())
                    .plus(this.world.zones().disruptionAt(at.x(), at.y()));
            sum += Math.max(0f, 1f - penalty.attackSpeed()) * (1f - penalty.missChance())
                    * Math.max(0f, 1f - penalty.rangeCells() / REACH_CELLS);
        }
        return sum / this.emplacements.size();
    }

    @Override
    public void accept(int time) {
        EnemyMob[] mobs = this.enemies.getEnemies();
        if (this.loadout.reveals()) {
            for (EnemyMob mob : mobs) {
                mob.applyEffect(Effect.revealed(REVEAL_TICKS, DamageSink.none()));
            }
        }
        this.hold(mobs, time);
        double strength = this.strength();
        this.burnStrength += strength / BURN_INTERVAL_TICKS;
        float heavy = HEAVY_HIT_POINTS * this.loadout.hitScale();
        float small = BLAST_HIT_POINTS * this.loadout.hitScale();
        this.fire(PowerProbe.SHOT, heavy, strength, mobs,
                target -> target.takeHit(Damage.physical(DamageUnits.ofPoints(heavy)), SHOT));
        this.fire(PowerProbe.BOLT, heavy, strength, mobs,
                target -> target.takeHit(Damage.magic(DamageUnits.ofPoints(heavy)), BOLT));
        this.fire(PowerProbe.BLAST, small, strength, mobs, target -> this.blast(target, mobs, small));
        if (time % BURN_INTERVAL_TICKS == 0) {
            this.burn(mobs, this.burnStrength);
            this.burnStrength = 0;
        }
    }

    private void hold(EnemyMob[] mobs, int time) {
        if (this.loadout.hold() == Hold.NONE || time % CHILL_INTERVAL_TICKS != 0) {
            return;
        }
        boolean freezes = this.loadout.hold() == Hold.CHILL_AND_FREEZE && time % FREEZE_INTERVAL_TICKS == 0;
        for (EnemyMob mob : mobs) {
            if (mob.validTarget()) {
                mob.applyEffect(Effect.chill(CHILL_STRENGTH, CHILL_TICKS, DamageSink.none()));
                if (freezes) {
                    mob.applyEffect(Effect.freeze(FREEZE_TICKS, DamageSink.none()));
                }
            }
        }
    }

    /** Lands {@code probe}'s hits owed this tick, each on the enemy furthest along that a tower could aim at. */
    private void fire(PowerProbe probe, float hitPoints, double strength, EnemyMob[] mobs, Consumer<EnemyMob> hit) {
        double perSecond = this.loadout.intensity(probe) * strength;
        if (perSecond <= 0) {
            return;
        }
        double due = this.owed.getOrDefault(probe, 0.0) + perSecond / hitPoints / TickRate.TICKS_PER_SECOND;
        int fired = 0;
        while (due >= 1.0 && fired < MOST_HITS_PER_TICK) {
            EnemyMob target = furthest(mobs);
            if (target == null) {
                break;
            }
            hit.accept(target);
            due -= 1.0;
            fired++;
        }
        // A tower with nothing to aim at holds one shot ready, no more.
        this.owed.put(probe, due >= 1.0 ? 1.0 : due);
    }

    private void blast(EnemyMob centre, EnemyMob[] mobs, float hitPoints) {
        double x = centre.getX();
        double y = centre.getY();
        for (EnemyMob mob : mobs) {
            if (mob.validTarget() && Math.hypot(mob.getX() - x, mob.getY() - y) <= this.blastRadius) {
                mob.takeHit(Damage.physical(DamageUnits.ofPoints(hitPoints)), BLAST);
            }
        }
    }

    private void burn(EnemyMob[] mobs, double strength) {
        double perSecond = this.loadout.intensity(PowerProbe.BURN) * strength;
        int perTick = (int) Math.round(DamageUnits.ofPoints((float) perSecond) / TickRate.TICKS_PER_SECOND);
        if (perTick <= 0) {
            return;
        }
        for (EnemyMob mob : mobs) {
            if (mob.validTarget()) {
                mob.applyEffect(Effect.burn(Damage.magic(perTick), BURN_TICKS, damage -> mob.takeHit(damage, BURNING)));
            }
        }
    }

    private static EnemyMob furthest(EnemyMob[] mobs) {
        EnemyMob best = null;
        for (EnemyMob mob : mobs) {
            if (mob.canBeTargeted() && (best == null || mob.getProgression() > best.getProgression())) {
                best = mob;
            }
        }
        return best;
    }

    /** What holds enemies back while the probes hit them. */
    enum Hold {
        NONE, CHILL, CHILL_AND_FREEZE
    }

    /**
     * What a rig lands: each probe's raw damage a second before the enemy's defences, what holds the enemies back, and
     * whether every enemy is kept revealed. A hit is {@code hitScale} times its size, as the hits of upgraded towers
     * are, at the same damage a second.
     */
    record Loadout(Map<PowerProbe, Double> perSecond, Hold hold, boolean reveals, float hitScale) {

        Loadout {
            Map<PowerProbe, Double> copy = new EnumMap<>(PowerProbe.class);
            copy.putAll(perSecond);
            perSecond = Collections.unmodifiableMap(copy);
        }

        static Loadout none() {
            return new Loadout(new EnumMap<>(PowerProbe.class), Hold.NONE, false, 1f);
        }

        static Loadout only(PowerProbe probe, double perSecond) {
            return none().with(probe, perSecond);
        }

        Loadout with(PowerProbe probe, double perSecond) {
            Map<PowerProbe, Double> more = new EnumMap<>(PowerProbe.class);
            more.putAll(this.perSecond);
            more.put(probe, perSecond);
            return new Loadout(more, this.hold, this.reveals, this.hitScale);
        }

        Loadout holding(Hold hold) {
            return new Loadout(this.perSecond, hold, this.reveals, this.hitScale);
        }

        Loadout revealing() {
            return new Loadout(this.perSecond, this.hold, true, this.hitScale);
        }

        Loadout hitting(float hitScale) {
            return new Loadout(this.perSecond, this.hold, this.reveals, hitScale);
        }

        Loadout scaledBy(double factor) {
            Map<PowerProbe, Double> scaled = new EnumMap<>(PowerProbe.class);
            this.perSecond.forEach((probe, value) -> scaled.put(probe, value * factor));
            return new Loadout(scaled, this.hold, this.reveals, this.hitScale);
        }

        double intensity(PowerProbe probe) {
            return this.perSecond.getOrDefault(probe, 0.0);
        }
    }
}
