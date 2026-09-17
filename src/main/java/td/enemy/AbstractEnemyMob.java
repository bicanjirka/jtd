package td.enemy;

import td.damage.Damage;
import td.economy.EconomyDelta;
import td.effect.ActiveEffects;
import td.effect.Effect;
import td.effect.EffectKind;
import td.util.GameWorld;
import td.util.ThreadConfined;
import td.wave.ArcLengthPath;
import td.wave.PathPose;
import td.wave.Vec2;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Everything every enemy shares: spawn delay, movement along the level's path, health and
 * damage, and the death-fade animation's timing. {@link DefinedEnemyMob} adds a body scale,
 * a facing angle, and whatever behavioural twist its {@link Trait}s give it (see
 * {@link #absorb} and {@link HurtSpeedTrait}'s speed-up on damage).
 * <p>
 * Movement is real arc-length distance: {@link #doTick} advances {@code distanceIntoLap} by
 * {@code speed} pixels and resolves it through the shared {@link ArcLengthPath}, so a curved
 * or diagonal path moves a mob at the same real-world pace a straight one does. Reaching the
 * end charges the player a {@link EconomyDelta#leak} and kills the mob outright, the same
 * {@code dead}/fade path a combat kill uses, just without the bounty - see {@link #leak()}.
 * <p>
 * <strong>Everything a mob is born with is set by this constructor and is {@code final}.</strong>
 * A mob used to be built in two phases - an empty constructor, then a {@code doInit} a leaf had
 * to remember to call last - which left the whole object mutable and visible half-built. The
 * compiler enforces the ordering now, the same way it does for {@code td.tower.AbstractTower}.
 * <p>
 * Everything that remains mutable is per-tick simulation state, and it is {@code private}: a
 * mob is owned by the {@code game-loop} thread and published to that thread by the
 * {@code CopyOnWriteArrayList} it is added to. Fourteen {@code protected} mutable fields meant
 * no invariant here could survive a subclass; a leaf now reaches state through the accessors
 * below. See CLAUDE.md 3 and 5.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
// per-tick simulation state; the frame build that reads it runs on the loop thread too
public abstract class AbstractEnemyMob implements EnemyMob {

    /**
     * Pixels per tick for a mob whose definition names no speed of its own. Rescaled from the
     * old fixed-point model (speed=40 meant "40/1000 of the current segment per tick", which -
     * since every segment was exactly one 32px cell - worked out to 40/1000*32 = 1.28 px/tick),
     * so every enemy's actual speed is unchanged; only the unit it is expressed in is.
     */
    public static final float DEFAULT_SPEED = 1.28f;
    /**
     * Health is stored in hundredths, matching the scale {@code td.tower} expresses damage in
     * ({@code SniperTower.DAMAGE} of {@code 4000} is 40 points a shot). Storing the fine-grained
     * unit is what lets a percentage resistance or a damage-over-time tick subtract a fraction
     * of a point without rounding to nothing.
     */
    private static final int HEALTH_UNITS_PER_POINT = 100;

    // Injected collaborators and the constants a mob is born with. All final, all set below.
    protected final GameWorld gameWorld;
    protected final int level;
    private final Type type;
    private final int price;
    private final int healthMax;
    private final ActiveEffects activeEffects = new ActiveEffects();
    /**
     * The path this mob measures its progress along, empty for a degenerate path - fewer than
     * two points, as a world has before any level is installed. An empty one has nothing to
     * measure distance along, so the mob holds at {@link #stationaryPosition} instead of moving.
     */
    private final Optional<ArcLengthPath> arcLengthPath;
    private final Vec2 stationaryPosition;
    /**
     * This mob's formation offset, fixed in <em>world</em> space and computed once, in the
     * constructor, from {@code spawnParameters.localOffset()} rotated by the path's facing at
     * the spawn point - zero for a normal spawn. Deliberately not recomputed from the path's
     * *current* tangent on every tick: that would make a shaped member's own position pivot
     * around the centerline as the path curves (distorting its effective speed) and jump
     * outright at an unrounded corner, where the tangent itself is discontinuous. Translating
     * the centerline by a constant vector has neither problem - same arc-length speed as the
     * centerline, always, and nothing to be discontinuous at. Applied in {@link #updatePosition()},
     * clamped to the board so a shaped formation never silently drifts into permanent
     * untargetability.
     */
    private final double offsetX;
    private final double offsetY;

    // Per-tick simulation state, owned by the game-loop thread. Private: a leaf that needs one
    // of these goes through an accessor, so this class can hold an invariant over them.
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
    // Set synchronously in doDamage() (which has no gameTime to record against) and captured
    // as criticalHitTick on this mob's own next doTick - the same deferred-capture shape
    // deathTick already uses, and for the same reason: a hit can land during another phase of
    // the same game tick (see td/enemy/CLAUDE.md's death-timing invariant), so "a critical hit
    // landed" and "this tick is the one to report it on" are not necessarily the same tick.
    private boolean criticalHitPending;
    private int criticalHitTick = -1;
    private double distanceIntoLap = 0;
    private double lastFacingRadians = 0;

    /**
     * Binds this mob to a world, its type and speed, and a starting position on its path.
     * <p>
     * A leaf passes its own constants straight through - there is no second initialization
     * step to remember, and no window in which a half-built mob is reachable. Anything a leaf
     * derives from the board scale or from {@code level} (a body scale, a speed curve) it
     * computes after this returns, by which point every field here is set.
     *
     * @param speed           this mob's own px/tick speed - already folded in with any
     *                        {@code SpawnShape} speed multiplier, since {@code spawnParameters}
     *                        only carries the tick countdown that multiplier implied
     * @param spawnParameters this mob's spawn delay (already converted to a tick countdown),
     *                        health (in whole points; stored internally in hundredths) and bounty
     */
    protected AbstractEnemyMob(GameWorld gameWorld, Type type, float speed, SpawnParameters spawnParameters, int level) {
        this.gameWorld = gameWorld;
        this.type = type;
        this.speed = speed;
        this.price = spawnParameters.price();
        this.level = level;
        this.health = spawnParameters.health() * HEALTH_UNITS_PER_POINT;
        this.healthMax = spawnParameters.health() * HEALTH_UNITS_PER_POINT;
        this.arcLengthPath = ArcLengthPath.of(gameWorld.getPath());
        List<Vec2> pathPoints = gameWorld.getPath().points();
        this.stationaryPosition = pathPoints.isEmpty() ? new Vec2(0, 0) : pathPoints.getFirst();
        // Every mob's distanceIntoLap starts at 0 regardless of delay or shape, so poseAt(0) is
        // always the true spawn point - this is the one and only place the offset's rotation is
        // computed, ever.
        double spawnFacing = this.arcLengthPath.map(path -> path.poseAt(0).facingRadians()).orElse(0.0);
        double cos = Math.cos(spawnFacing);
        double sin = Math.sin(spawnFacing);
        Vec2 localOffset = spawnParameters.localOffset();
        this.offsetX = localOffset.x() * cos - localOffset.y() * sin;
        this.offsetY = localOffset.x() * sin + localOffset.y() * cos;
        this.delay = spawnParameters.delayTicks();
        // Keyed off the converted tick count, not the raw slot position that produced it: a
        // fractional per-member delay (see SpawnShape's column/drip spacing) can round down to
        // zero ticks from a nonzero position, and doTick's inactive branch only ever counts
        // down from delay > 0 - basing this on the raw position instead could leave such a mob
        // inactive forever.
        this.inactive = this.delay > 0;
        this.validTarget = !this.inactive;
        this.updatePosition();
        // No prior tick to interpolate from at spawn - start with prev == current.
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
     * Applies a hit, paying the player its bounty and removing it from the roster's alive
     * count if this kills it. A hit on an already-dead mob is a no-op - several towers can
     * fire into the same mob within one tick, and only the first may count as the kill (see
     * {@code AbstractTower.dealDamage}, which relies on that).
     *
     * @return the damage that actually landed: {@link #absorb}'s result capped at the health
     * this mob had left, and {@link Damage#none()} for a mob that is already dead or not
     * currently targetable. The cap means a killing blow reports only the health it actually
     * removed, so overkill is not credited to whoever fired it.
     */
    public Damage doDamage(Damage damage) {
        if (this.dead) {
            return Damage.none();
        }
        Damage landed = Damage.none();
        if (this.validTarget()) {
            landed = this.absorb(damage).cappedAt(this.health);
            this.health -= landed.amount();
            if (landed.critical()) {
                this.criticalHitPending = true;
            }
        }
        if (this.health <= 0) {
            this.validTarget = false;
            this.dead = true;
            this.gameWorld.economy().apply(EconomyDelta.kill(this.price));
            this.gameWorld.enemies().reportDeath();
        }
        return landed;
    }

    /**
     * Hook for a mob that resists part of an incoming hit (see {@link DefinedEnemyMob#absorb},
     * which folds every {@link Trait#onHit}). The default is no resistance - the damage lands
     * unchanged.
     */
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
     * This mob's x/y as of the tick before last, i.e. the interpolation source for a render
     * landing between two ticks. Equal to getX()/getY() at spawn, where there is nothing
     * meaningful to interpolate from.
     */
    public double getPrevX() {
        return this.prevX;
    }

    public double getPrevY() {
        return this.prevY;
    }

    /**
     * This mob's speed, folding in every currently active speed-affecting {@link Effect}
     * (see {@link #getSpeed()}). {@code speed} itself stays the intrinsic value - the one
     * {@link DefinedEnemyMob#doDamage} recomputes from its traits' {@code speedFactor} (and,
     * for a shaped spawn, its {@code SpawnShape}'s own speed multiplier) - so a slow or freeze
     * never gets permanently baked into it, and never gets wiped out the next time a trait
     * recomputes it.
     */
    private float effectiveSpeed() {
        return this.speed * this.activeEffects.speedMultiplier();
    }

    public float getSpeed() {
        return this.effectiveSpeed();
    }

    /**
     * Replaces this mob's intrinsic speed - what a {@link Trait}'s {@code speedFactor}
     * recomputes when the mob is hurt (see {@link DefinedEnemyMob#doDamage}). Deliberately the
     * intrinsic value and not the effective one: a slow or a freeze multiplies this at read
     * time (see {@link #effectiveSpeed()}), so recomputing here never bakes a status effect in
     * permanently and never wipes one out.
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

    /**
     * A coarse "how far into this lap of the path" ranking value, used only to compare two
     * enemies on the same path (see FurthestAlongPathSelector) - sub-pixel precision has no
     * practical effect on that ranking, so this stays an int rather than widening to match
     * the double-precision position/distance it's derived from.
     */
    public int getProgression() {
        return (int) this.distanceIntoLap;
    }

    /**
     * The exact, sub-pixel distance into the path's current lap - see {@link #jumpToDistance}.
     */
    protected double getDistanceIntoLap() {
        return this.distanceIntoLap;
    }

    /**
     * Places this mob at an arbitrary point along the path instead of the start every mob
     * otherwise spawns at - what an ability-driven spawn (the Warden's egg, a reinforcement)
     * uses to appear where the spawning mob actually was, via
     * {@code DefinedEnemyMob.spawnAtSamePositionAs}.
     */
    protected void jumpToDistance(double distanceIntoLap) {
        this.distanceIntoLap = distanceIntoLap;
        this.updatePosition();
        this.prevX = this.x;
        this.prevY = this.y;
    }

    /**
     * Spawned, on the board, and still alive - the precondition every targeting query applies.
     */
    public boolean validTarget() {
        return ((!this.inactive) && this.validTarget && (!this.dead));
    }

    public boolean validTarget(Type type) {
        return (this.validTarget() && (type.equals(this.type)));
    }

    public boolean validTarget(Type type1, Type type2) {
        return (this.validTarget(type1) || this.validTarget(type2));
    }

    public boolean isDead() {
        return this.dead;
    }

    /**
     * Ticks elapsed since this mob died, or -1 while still alive. Death timing is
     * captured here (the first doTick call after doDamage() sets dead=true) rather
     * than lazily inside paint(), so the death-fade animation advances with the
     * simulation clock instead of with however often the board happens to repaint.
     */
    public int ticksSinceDeath(int gameTime) {
        return this.deathTick < 0 ? -1 : gameTime - this.deathTick;
    }

    public int fadeDurationTicks() {
        return 3 * this.level + 6;
    }

    /**
     * Ticks elapsed since this mob was last observed to survive a critical hit, or {@code -1}
     * if it never has. Mirrors {@link #ticksSinceDeath} exactly, including the deferred-capture
     * reason: {@code doDamage} has no {@code gameTime} to record against, so a landed critical
     * hit is captured as {@link #criticalHitTick} on this mob's own next {@link #doTick} instead.
     */
    public int ticksSinceCriticalHit(int gameTime) {
        return this.criticalHitTick < 0 ? -1 : gameTime - this.criticalHitTick;
    }

    public boolean isFadeComplete(int gameTime) {
        int age = this.ticksSinceDeath(gameTime);
        return age > this.fadeDurationTicks();
    }

    /**
     * ticksSinceDeath can be -1 (death recorded by doDamage() mid-tick, but this
     * mob's own doTick() hasn't run yet to capture deathTick) - clamp to a valid
     * Color alpha range rather than let that produce a value above 255.
     */
    public int fadeAlpha(int ticksSinceDeath) {
        int alpha = 255 - (ticksSinceDeath * (255 / (this.fadeDurationTicks() + 1)));
        return Math.min(255, Math.max(alpha, 0));
    }

    /**
     * Recomputes x/y (and, while moving, the precise path-facing angle) from the current
     * arcLengthPath/distanceIntoLap state, plus this mob's fixed {@link #offsetX}/{@link
     * #offsetY}. The offset is clamped onto the board only while the path's own position is
     * already on the board - an offset near an interior edge or corner would otherwise push a
     * mob outside {@link #doTick}'s validTarget bounds check and leave it silently untargetable
     * while still walking to the exit. Where the path's own position is authored off-board (a
     * level's spawn/despawn buffer - see {@link td.wave.Point}'s doc comment), it is left
     * unclamped instead, so an enemy visibly walks in from, and out to, off-screen rather than
     * popping into view already sitting at the edge. A degenerate path has nothing to measure
     * distance along, so it just holds at its one available point, offset the same fixed amount
     * as anywhere else - the offset needs no tangent to be relative to any more, so the
     * degenerate case needs no special-casing either.
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

    /**
     * The path's own facing direction at this mob's current position - exact, geometry-based,
     * not derived from a pixel delta over one tick (see {@link PathDirectionalMovement}).
     */
    protected double getPathFacingRadians() {
        return this.lastFacingRadians;
    }

    /**
     * Counts down the spawn delay, or - for a live mob - resolves this tick's active status
     * effects and then advances it along the path, or - once dead - records the tick death
     * happened on so the fade can be timed against the simulation clock rather than the
     * repaint rate. A damage-over-time effect that kills the mob this tick skips movement
     * entirely for the rest of this call - there is nothing left to move.
     */
    public void doTick(int gameTime) {
        if (this.criticalHitPending) {
            this.criticalHitPending = false;
            this.criticalHitTick = gameTime;
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
            // Read this tick's speed multiplier before ticking durations down, so an effect
            // entering the last tick of its duration still suppresses this tick's movement -
            // ActiveEffects.tick() removes it before returning, and querying afterward would
            // silently shorten its effect on movement by one tick relative to its effect on
            // damage-over-time (which it applies before removing itself either way).
            float speedMultiplier = this.activeEffects.speedMultiplier();
            this.activeEffects.tick();
            if (this.dead) {
                // A damage-over-time tick just killed this mob - doDamage() already set
                // dead=true synchronously (its sink calls straight back into doDamage()).
                // Movement must not run this tick: there is nothing left to move.
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
     * Reaching the path's end costs a life and docks the score by exactly the bounty this mob
     * would have paid on a kill - {@link EconomyDelta#leak}, mirroring {@link EconomyDelta#kill}
     * - but the mob does not loop back to the path's start to try again - it goes through the
     * exact {@code dead}/fade/{@code reportDeath()} path a combat kill does, just with a leak
     * penalty instead of a bounty. That is the actual punishment: gone for good means no tower
     * ever gets a second chance to kill it for its bounty. A price-0 mob (a wave authored to pay
     * no bounty at all) still costs the life; it was never going to cost any score either way,
     * on a kill or a leak.
     */
    private void leak() {
        this.validTarget = false;
        this.dead = true;
        this.gameWorld.economy().apply(EconomyDelta.leak(this.price));
        this.gameWorld.enemies().reportDeath();
    }
}
