package td.zone;

import td.damage.Damage;
import td.effect.Effect;
import td.enemy.EnemyMob;
import td.util.ThreadConfined;
import td.util.TickRate;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A disc on the board that keeps working after the shot that made it: every pulse it puts its
 * kind's effects on each enemy inside, credited to the tower that made it. Its pulses are periodic,
 * so they never crit.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class Zone {

    private static final int BURN_TICKS = 60;
    private static final int POISON_TICKS = 60;
    /** Tar lets go of an enemy this long after its last pulse. */
    private static final int TARRED_TICKS = 2 * ZoneRoster.PULSE_TICKS;
    private static final float FROST_CHILL = 0.2f;
    private static final int FROST_CHILL_TICKS = 40;
    /** Pulses in a row inside a frost zone before it freezes what stands there. */
    private static final int FROST_PULSES_TO_FREEZE = 4;
    private static final int FROST_FREEZE_TICKS = Math.round(TickRate.TICKS_PER_SECOND * 1.5f);

    private final ZoneKind kind;
    private final double x;
    private final double y;
    private final float radius;
    private final int lifetimeTicks;
    private final int strength;
    private final ZoneOwner owner;
    private final Map<EnemyMob, Integer> pulsesInside = new IdentityHashMap<>();
    private int ageTicks;

    /**
     * @param radius   pixels
     * @param strength a burn's or poison's damage a tick, in damage units; unused by the kinds with none
     */
    public Zone(ZoneKind kind, double x, double y, float radius, int lifetimeTicks, int strength, ZoneOwner owner) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.lifetimeTicks = lifetimeTicks;
        this.strength = strength;
        this.owner = owner;
    }

    public ZoneKind kind() {
        return this.kind;
    }

    public double x() {
        return this.x;
    }

    public double y() {
        return this.y;
    }

    public float radius() {
        return this.radius;
    }

    /** How much of its life is left, from 1 when it was made to 0 when it ends. */
    public float lifeLeft() {
        return Math.max(0f, 1f - (float) this.ageTicks / this.lifetimeTicks);
    }

    int ageTicks() {
        return this.ageTicks;
    }

    void age() {
        this.ageTicks++;
    }

    boolean isExpired() {
        return this.ageTicks >= this.lifetimeTicks;
    }

    /** Puts this pulse's effects on {@code enemy}, which stands inside. */
    void touch(EnemyMob enemy) {
        int pulses = this.pulsesInside.merge(enemy, 1, Integer::sum);
        switch (this.kind) {
            case BURNING_GROUND -> this.owner.applyEffect(enemy,
                    sink -> Effect.burn(Damage.magic(this.strength), BURN_TICKS, sink));
            case TAR -> {
                this.owner.applyEffect(enemy, sink -> Effect.tarred(TARRED_TICKS, sink));
                this.owner.applyEffect(enemy, sink -> Effect.poison(Damage.magic(this.strength), POISON_TICKS, sink));
            }
            case FROST_GROUND -> {
                this.owner.applyEffect(enemy, sink -> Effect.chill(FROST_CHILL, FROST_CHILL_TICKS, sink));
                if (pulses >= FROST_PULSES_TO_FREEZE) {
                    this.pulsesInside.put(enemy, 0);
                    this.owner.applyEffect(enemy, sink -> Effect.freeze(FROST_FREEZE_TICKS, sink));
                }
            }
        }
    }

    /** Whoever was inside before and is not among {@code inside} now starts over. */
    void keepOnly(List<EnemyMob> inside) {
        this.pulsesInside.keySet().removeIf(enemy -> !inside.contains(enemy));
    }
}
