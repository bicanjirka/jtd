package td.zone;

import td.effect.Effect;
import td.enemy.EnemyMob;
import td.stat.DisruptionPenalty;
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

    /** A lifetime for a zone that stays until the wave is over. */
    public static final int UNTIL_WAVE_END = Integer.MAX_VALUE;
    /** A ward mine ignores whatever is on it this long after it is laid, so the enemy that laid it walks off first. */
    private static final int WARD_ARMING_TICKS = Math.round(TickRate.TICKS_PER_SECOND);

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
    private final ZoneTrigger trigger;
    private final List<Effect> carried;
    private final DisruptionPenalty penalty;
    private final Map<EnemyMob, Integer> pulsesInside = new IdentityHashMap<>();
    private int ageTicks;
    private boolean triggered;

    /**
     * @param radius   pixels
     * @param strength a burn's or poison's damage a tick, in damage units; unused by the kinds with none
     */
    public Zone(ZoneKind kind, double x, double y, float radius, int lifetimeTicks, int strength, ZoneOwner owner) {
        this(kind, x, y, radius, lifetimeTicks, strength, owner, (mine, by) -> {
        }, List.of(), DisruptionPenalty.none());
    }

    private Zone(ZoneKind kind, double x, double y, float radius, int lifetimeTicks, int strength, ZoneOwner owner,
                 ZoneTrigger trigger, List<Effect> carried, DisruptionPenalty penalty) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.lifetimeTicks = lifetimeTicks;
        this.strength = strength;
        this.owner = owner;
        this.trigger = trigger;
        this.carried = List.copyOf(carried);
        this.penalty = penalty;
    }

    /** A mine that {@code trigger} goes off the first time an enemy steps within {@code radius} of it. */
    public static Zone mine(double x, double y, float radius, int lifetimeTicks, ZoneOwner owner, ZoneTrigger trigger) {
        return new Zone(ZoneKind.MINE, x, y, radius, lifetimeTicks, 0, owner, trigger, List.of(),
                DisruptionPenalty.none());
    }

    /** A mine that {@code trigger} sets off, armed a moment after it is laid, for the enemy that steps on it. */
    public static Zone wardMine(double x, double y, float radius, int lifetimeTicks, ZoneOwner owner,
                                ZoneTrigger trigger) {
        return new Zone(ZoneKind.WARD_MINE, x, y, radius, lifetimeTicks, 0, owner, trigger, List.of(),
                DisruptionPenalty.none());
    }

    /** Ground that heals {@code unitsPerTick} (in damage units) each tick, for as long as an enemy stands on it. */
    public static Zone healing(double x, double y, float radius, int lifetimeTicks, int unitsPerTick, ZoneOwner owner) {
        return new Zone(ZoneKind.HEALING_GROUND, x, y, radius, lifetimeTicks, unitsPerTick, owner, (mine, by) -> {
        }, List.of(), DisruptionPenalty.none());
    }

    public static Zone haste(double x, double y, float radius, int lifetimeTicks, ZoneOwner owner) {
        return new Zone(ZoneKind.HASTE_GROUND, x, y, radius, lifetimeTicks, 0, owner, (mine, by) -> {
        }, List.of(), DisruptionPenalty.none());
    }

    /** A field that jams every tower within {@code radius} by {@code penalty}. */
    public static Zone blinding(double x, double y, float radius, int lifetimeTicks, DisruptionPenalty penalty,
                                ZoneOwner owner) {
        return new Zone(ZoneKind.BLINDING_FIELD, x, y, radius, lifetimeTicks, 0, owner, (mine, by) -> {
        }, List.of(), penalty);
    }

    /** A cloud that gives every enemy inside the {@code carried} effects. */
    public static Zone cloud(double x, double y, float radius, int lifetimeTicks, ZoneOwner owner,
                             List<Effect> carried) {
        return new Zone(ZoneKind.CURSED_CLOUD, x, y, radius, lifetimeTicks, 0, owner, (mine, by) -> {
        }, carried, DisruptionPenalty.none());
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

    /** What the towers within {@code radius} suffer; nothing for any kind but a blinding field. */
    public DisruptionPenalty penalty() {
        return this.penalty;
    }

    /** Whether it holds until the wave is over rather than for a time. */
    boolean lastsUntilWaveEnd() {
        return this.lifetimeTicks == UNTIL_WAVE_END;
    }

    /** Whether it can set off yet: only a ward mine waits to be armed. */
    boolean isArmed() {
        return this.kind != ZoneKind.WARD_MINE || this.ageTicks >= WARD_ARMING_TICKS;
    }

    boolean covers(double px, double py) {
        double dx = this.x - px;
        double dy = this.y - py;
        return dx * dx + dy * dy <= (double) this.radius * this.radius;
    }

    int ageTicks() {
        return this.ageTicks;
    }

    void age() {
        this.ageTicks++;
    }

    boolean isExpired() {
        return this.triggered || this.ageTicks >= this.lifetimeTicks;
    }

    /** A mine goes off for {@code by}, once, and is spent. */
    void detonate(EnemyMob by) {
        if (!this.triggered) {
            this.triggered = true;
            this.trigger.triggered(this, by);
        }
    }

    /** Puts this pulse's effects on {@code enemy}, which stands inside. */
    void touch(EnemyMob enemy) {
        int pulses = this.pulsesInside.merge(enemy, 1, Integer::sum);
        ZoneEffects.touch(this.kind, this.strength, this.owner, enemy);
        this.carried.stream().flatMap(effect -> effect.afterTicks(this.ageTicks).stream()).forEach(enemy::applyEffect);
        if (this.kind == ZoneKind.FROST_GROUND && pulses >= FROST_PULSES_TO_FREEZE) {
            this.pulsesInside.put(enemy, 0);
            this.owner.applyEffect(enemy, sink -> Effect.freeze(FROST_FREEZE_TICKS, sink));
        }
    }

    /** Whoever was inside before and is not among {@code inside} now starts over. */
    void keepOnly(List<EnemyMob> inside) {
        this.pulsesInside.keySet().removeIf(enemy -> !inside.contains(enemy));
    }
}
