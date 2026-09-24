package td.enemy;

import td.damage.Damage;
import td.economy.EconomyDelta;
import td.effect.ActiveEffects;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.EffectTransitions;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.wave.ArcLengthPath;
import td.wave.Path;
import td.wave.PathPose;
import td.wave.Vec2;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * What every enemy shares: spawn delay, movement along its path, health, damage and death-fade
 * timing.
 * <p>
 * Movement is arc-length distance along an {@link ArcLengthPath}, so curves and diagonals move at
 * the same pace as straight legs. Reaching the end charges a leak and kills the mob through the
 * same fade path as a combat kill.
 * <p>
 * Everything a mob is born with is {@code final} and set by the constructor. Mutable per-tick state
 * is private and owned by the game-loop thread; subclasses reach it through accessors.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public abstract class AbstractEnemyMob implements EnemyMob {

    /** Pixels per tick for a mob whose definition names no speed. */
    public static final float DEFAULT_SPEED = 1.28f;
    /**
     * Health is stored in hundredths, the unit towers deal damage in, so a percentage resist or a
     * damage-over-time tick can remove a fraction of a point.
     */
    private static final int HEALTH_UNITS_PER_POINT = 100;
    /**
     * Scale of {@link #getProgression()}: fine enough that mobs a fraction of a percent apart still
     * compare distinctly.
     */
    private static final int PROGRESSION_SCALE = 1_000_000;

    protected final GameWorld gameWorld;
    protected final Rank rank;
    private final Type type;
    private final int price;
    private final int healthMax;
    private final int pathIndex;
    private final ActiveEffects activeEffects = new ActiveEffects();
    private final EffectTransitions effectTransitions = new EffectTransitions();
    /**
     * Empty for a path with fewer than two points; the mob then holds at
     * {@link #stationaryPosition}.
     */
    private final Optional<ArcLengthPath> arcLengthPath;
    private final Vec2 stationaryPosition;
    /**
     * Formation offset in world space, fixed at spawn from the path's facing there. Not re-rotated
     * with the path's tangent each tick: that would warp a member's speed on curves and make it
     * jump at sharp corners.
     */
    private final double offsetX;
    private final double offsetY;

    private int health;
    private boolean inactive;
    private boolean validTarget;
    private boolean dead = false;
    private float speed;
    private double x;
    private double y;
    private double prevX;
    private double prevY;
    private int delay;
    private int deathTick = -1;
    // Captured on the next doTick: doDamage() has no gameTime, and a hit can land in another
    // phase of the same tick.
    private boolean criticalHitPending;
    private int criticalHitTick = -1;
    // Same deferred capture as criticalHitPending, for any landed damage.
    private boolean damageTakenPending;
    private int damageTakenTick = -1;
    // Null until the first cast; never exposed as null.
    private AbilityCast lastAbilityCast;
    private int abilitySpawnTick = -1;
    private double distanceIntoLap = 0;
    private double lastFacingRadians = 0;

    /**
     * @param speed           px/tick, with any spawn-shape multiplier already applied
     * @param spawnParameters spawn delay in ticks, health in whole points, and bounty
     */
    protected AbstractEnemyMob(GameWorld gameWorld, Type type, float speed, SpawnParameters spawnParameters, Rank rank) {
        this.gameWorld = gameWorld;
        this.type = type;
        this.speed = speed;
        this.price = spawnParameters.price();
        this.rank = rank;
        this.health = spawnParameters.health() * HEALTH_UNITS_PER_POINT;
        this.healthMax = spawnParameters.health() * HEALTH_UNITS_PER_POINT;
        this.pathIndex = spawnParameters.pathIndex();
        Path path = gameWorld.level().pathAt(this.pathIndex);
        this.arcLengthPath = ArcLengthPath.of(path);
        List<Vec2> pathPoints = path.points();
        this.stationaryPosition = pathPoints.isEmpty() ? new Vec2(0, 0) : pathPoints.getFirst();
        double spawnFacing = this.arcLengthPath.map(arcPath -> arcPath.poseAt(0).facingRadians()).orElse(0.0);
        double cos = Math.cos(spawnFacing);
        double sin = Math.sin(spawnFacing);
        Vec2 localOffset = spawnParameters.localOffset();
        this.offsetX = localOffset.x() * cos - localOffset.y() * sin;
        this.offsetY = localOffset.x() * sin + localOffset.y() * cos;
        this.delay = spawnParameters.delayTicks();
        // From the rounded tick count, not the slot position: a fractional delay can round to zero
        // ticks, and an inactive mob with no delay to count down would never activate.
        this.inactive = this.delay > 0;
        this.validTarget = !this.inactive;
        this.updatePosition();
        this.prevX = this.x;
        this.prevY = this.y;
    }

    public int getHealth() {
        return this.health;
    }

    public int getBounty() {
        return this.price;
    }

    public float getHealthFraction() {
        return (float) this.health / this.healthMax;
    }

    public boolean isInactive() {
        return this.inactive;
    }

    /**
     * Applies a hit, paying the bounty if it kills. A no-op on a dead mob, so only the first of
     * several same-tick hits counts as the kill.
     *
     * @return the damage that landed: after resistance and shield, capped at remaining health so
     * overkill is not credited, and {@link Damage#none()} if the mob is dead or untargetable
     */
    public Damage doDamage(Damage damage) {
        if (this.dead) {
            return Damage.none();
        }
        Damage landed = Damage.none();
        if (this.validTarget()) {
            landed = this.activeEffects.applyShield(this.absorb(damage)).cappedAt(this.health);
            this.health -= landed.amount();
            if (landed.amount() > 0) {
                this.damageTakenPending = true;
            }
            if (landed.critical()) {
                this.criticalHitPending = true;
            }
        }
        if (this.health <= 0) {
            this.validTarget = false;
            this.dead = true;
            int score = Math.round(this.price * this.rank.scoreMultiplier());
            this.gameWorld.economy().apply(EconomyDelta.kill(this.price, score));
            this.gameWorld.enemies().reportDeath();
        }
        return landed;
    }

    /** Hook for resisting part of a hit; by default the damage lands unchanged. */
    protected Damage absorb(Damage incoming) {
        return incoming;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    /**
     * Position as of the previous tick, the interpolation source; equal to the current position at
     * spawn.
     */
    public double getPrevX() {
        return this.prevX;
    }

    public double getPrevY() {
        return this.prevY;
    }

    /**
     * Intrinsic speed times the active effects' multiplier, so an effect is never baked into
     * {@code speed}.
     */
    private float effectiveSpeed() {
        return this.speed * this.activeEffects.speedMultiplier();
    }

    public float getSpeed() {
        return this.effectiveSpeed();
    }

    /**
     * Replaces the intrinsic speed. Effects multiply it at read time, so this never bakes one in or
     * wipes one out.
     */
    protected void setSpeed(float speed) {
        this.speed = speed;
    }

    public void applyEffect(Effect effect) {
        this.activeEffects.apply(effect);
    }

    public Set<EffectKind> activeEffectKinds() {
        return this.activeEffects.activeKinds();
    }

    /** {@code -1} if never gained. */
    public int ticksSinceEffectGained(EffectKind kind, int gameTime) {
        return this.effectTransitions.ticksSinceGained(kind, gameTime);
    }

    /** {@code -1} if never lost. */
    public int ticksSinceEffectLost(EffectKind kind, int gameTime) {
        return this.effectTransitions.ticksSinceLost(kind, gameTime);
    }

    /**
     * Distance into the current lap as a fraction of this mob's own path length, so mobs on paths
     * of different lengths rank by how close they are to leaking.
     */
    public int getProgression() {
        return this.arcLengthPath
                .map(path -> (int) Math.round(this.distanceIntoLap / path.totalLength() * PROGRESSION_SCALE))
                .orElse(0);
    }

    protected double getDistanceIntoLap() {
        return this.distanceIntoLap;
    }

    /** Passed on by an ability spawn so the new mob stays on its parent's path. */
    protected int getPathIndex() {
        return this.pathIndex;
    }

    /**
     * Places this mob at any point along the path, so an ability spawn appears where its parent
     * was.
     */
    protected void jumpToDistance(double distanceIntoLap) {
        this.distanceIntoLap = distanceIntoLap;
        this.updatePosition();
        this.prevX = this.x;
        this.prevY = this.y;
    }

    /** Spawned, on the board and alive: the precondition every targeting query applies. */
    public boolean validTarget() {
        return ((!this.inactive) && this.validTarget && (!this.dead));
    }

    public boolean validTarget(Type type) {
        return (this.validTarget() && (type.equals(this.effectiveType())));
    }

    /**
     * {@link Type#INVISIBLE} while an invisibility effect is active, otherwise the authored type,
     * so any enemy can be made invisible by an effect.
     */
    private Type effectiveType() {
        return this.activeEffects.isInvisible() ? Type.INVISIBLE : this.type;
    }

    public boolean validTarget(Type type1, Type type2) {
        return (this.validTarget(type1) || this.validTarget(type2));
    }

    public boolean isDead() {
        return this.dead;
    }

    /**
     * {@code -1} while alive. Captured on the first {@code doTick} after death, so the fade follows
     * the simulation clock rather than the repaint rate.
     */
    public int ticksSinceDeath(int gameTime) {
        return this.deathTick < 0 ? -1 : gameTime - this.deathTick;
    }

    public int fadeDurationTicks() {
        return 3 * this.rank.ordinal() + 6;
    }

    /** Inherited by an ability spawn. */
    public Rank getRank() {
        return this.rank;
    }

    /**
     * {@code -1} if never. Captured on the next {@code doTick}, like {@link #ticksSinceDeath},
     * because {@code doDamage} has no game time.
     */
    public int ticksSinceCriticalHit(int gameTime) {
        return this.criticalHitTick < 0 ? -1 : gameTime - this.criticalHitTick;
    }

    /** {@code -1} if never. Captured like {@link #ticksSinceCriticalHit}. */
    public int ticksSinceDamageTaken(int gameTime) {
        return this.damageTakenTick < 0 ? -1 : gameTime - this.damageTakenTick;
    }

    public void recordAbilityCast(EffectKind kind, float radius, int gameTime) {
        this.lastAbilityCast = new AbilityCast(kind, radius, gameTime);
    }

    public Optional<AbilityCast> lastAbilityCast() {
        return Optional.ofNullable(this.lastAbilityCast);
    }

    public void recordAbilitySpawn(int gameTime) {
        this.abilitySpawnTick = gameTime;
    }

    /** {@code -1} for a mob that came from a wave. */
    public int ticksSinceAbilitySpawn(int gameTime) {
        return this.abilitySpawnTick < 0 ? -1 : gameTime - this.abilitySpawnTick;
    }

    public boolean isFadeComplete(int gameTime) {
        int age = this.ticksSinceDeath(gameTime);
        return age > this.fadeDurationTicks();
    }

    /**
     * Clamped, because {@code ticksSinceDeath} is {@code -1} between a mid-tick death and the next
     * {@code doTick}.
     */
    public int fadeAlpha(int ticksSinceDeath) {
        int alpha = 255 - (ticksSinceDeath * (255 / (this.fadeDurationTicks() + 1)));
        return Math.min(255, Math.max(alpha, 0));
    }

    /**
     * Recomputes position from the distance along the path plus the fixed offset. The offset is
     * clamped to the board only while the path point is on the board: clamping keeps a formation
     * member targetable, and skipping it off-board lets enemies walk in from and out to off-screen.
     */
    private void updatePosition() {
        if (this.arcLengthPath.isPresent()) {
            PathPose pose = this.arcLengthPath.get().poseAt(this.distanceIntoLap);
            this.x = clampOntoBoard(pose.position().x(), this.offsetX, this.gameWorld.getBoard().maxX());
            this.y = clampOntoBoard(pose.position().y(), this.offsetY, this.gameWorld.getBoard().maxY());
            this.lastFacingRadians = pose.facingRadians();
        } else {
            this.x = this.stationaryPosition.x() + this.offsetX;
            this.y = this.stationaryPosition.y() + this.offsetY;
        }
    }

    private static double clampOntoBoard(double pathPosition, double offset, int max) {
        if (pathPosition < 0 || pathPosition > max) {
            return pathPosition + offset;
        }
        return clamp(pathPosition + offset, 0, max);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    /** The path's exact facing at this mob's position, not derived from movement. */
    protected double getPathFacingRadians() {
        return this.lastFacingRadians;
    }

    /**
     * Counts down the spawn delay; for a live mob resolves effects and moves; once dead, records
     * the death tick. A damage-over-time kill skips movement.
     */
    public void doTick(int gameTime) {
        if (this.criticalHitPending) {
            this.criticalHitPending = false;
            this.criticalHitTick = gameTime;
        }
        if (this.damageTakenPending) {
            this.damageTakenPending = false;
            this.damageTakenTick = gameTime;
        }
        if (this.inactive) {
            if (this.delay > 0) {
                this.delay--;
                if (this.delay == 0) {
                    this.inactive = false;
                    this.validTarget = true;
                    this.doTick(gameTime);
                }
            }
        } else if (this.dead) {
            if (this.deathTick < 0) {
                this.deathTick = gameTime;
            }
        } else {
            // Read before tick(): an effect in its last tick must still slow this tick's movement.
            float speedMultiplier = this.activeEffects.speedMultiplier();
            // Applied here, not through tick()'s sink: a heal is not a negative damagePerTick.
            this.health = Math.min(this.healthMax, this.health + this.activeEffects.healPerTick());
            this.activeEffects.tick();
            // After tick() so an expiry is seen the tick it happens; before the dead-return so a lethal
            // damage-over-time tick still records the loss.
            this.effectTransitions.observe(this.activeEffects.activeKinds(), gameTime);
            if (this.dead) {
                return;
            }
            this.prevX = this.x;
            this.prevY = this.y;
            if (this.arcLengthPath.isPresent()) {
                this.distanceIntoLap += this.speed * speedMultiplier;
                if (this.distanceIntoLap >= this.arcLengthPath.get().totalLength()) {
                    this.leak();
                    return;
                }
            }
            this.updatePosition();
            this.validTarget = this.x >= 0 && this.x <= this.gameWorld.getBoard().maxX() && this.y >= 0 && this.y <= this.gameWorld.getBoard().maxY();
        }
    }

    /**
     * Costs a life and the score this mob's bounty would have paid, then kills it through the
     * normal fade path. It does not loop back, so no tower gets a second chance at its bounty.
     */
    private void leak() {
        this.validTarget = false;
        this.dead = true;
        this.gameWorld.economy().apply(EconomyDelta.leak(this.price));
        this.gameWorld.enemies().reportDeath();
    }
}
