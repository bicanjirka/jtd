package td.enemy;

import td.damage.Damage;
import td.economy.EconomyDelta;
import td.effect.ActiveEffects;
import td.effect.Effect;
import td.effect.EffectKind;
import td.util.GameWorld;
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
 * end wraps back to the start and charges the player a {@link EconomyDelta#leak} - a mob is
 * never removed from the roster by walking, only by dying.
 */
public abstract class AbstractEnemyMob implements EnemyMob {

    /**
     * Health is stored in hundredths, matching the scale {@code td.tower} expresses damage in
     * ({@code TowerOne.damage} of {@code 4000} is 40 points a shot). Storing the fine-grained
     * unit is what lets a percentage resistance or a damage-over-time tick subtract a fraction
     * of a point without rounding to nothing.
     */
    private static final int HEALTH_UNITS_PER_POINT = 100;

    protected type type;
    protected boolean inactive = true;
    protected boolean validTarget = false;
    protected boolean dead = false;
    protected int price;
    protected int level;
    protected double x, y;
    private double prevX, prevY;
    // Pixels per tick. Rescaled from the old fixed-point model (speed=40 meant "40/1000 of the
    // current segment per tick", which - since every segment was exactly one 32px cell - worked
    // out to 40/1000*32 = 1.28 px/tick), so every enemy's actual speed is unchanged; only the
    // unit it is expressed in is.
    protected float speed = 1.28f;
    protected float speedMax = 1.28f;
    protected final float speedBase = 1.28f;
    private final ActiveEffects activeEffects = new ActiveEffects();
    protected int health;
    protected int healthMax;
    protected GameWorld gameWorld;
    private int delay;
    private int deathTick = -1;
    private ArcLengthPath arcLengthPath;
    private Vec2 stationaryPosition;
    private double distanceIntoLap = 0;
    private double lastFacingRadians = 0;


    public AbstractEnemyMob() {
        this.type = EnemyMob.type.Normal;
    }

    /**
     * Binds this mob to a world and a starting position on its path. A subclass overriding
     * this must call {@code super.doInit} first - anything derived from the board scale or
     * from {@code level} (a body scale, a speed curve) reads fields this sets. {@code delay}
     * is the mob's slot index within its wave, converted here into a tick countdown before it
     * becomes active and targetable.
     */
    protected void doInit(GameWorld gameWorld, int delay, int health, int price, int level) {
        this.gameWorld = gameWorld;
        this.price = price;
        this.level = level;
        this.health = health * HEALTH_UNITS_PER_POINT;
        this.healthMax = health * HEALTH_UNITS_PER_POINT;
        Optional<ArcLengthPath> arcLength = ArcLengthPath.of(this.gameWorld.getPath());
        this.arcLengthPath = arcLength.orElse(null);
        // A degenerate path (fewer than two points - e.g. an empty placeholder GameWorld has
        // before any level loads) has nothing to measure distance along - hold at its one
        // available point (or the origin, if it has none at all) rather than move at all.
        List<Vec2> pathPoints = this.gameWorld.getPath().points();
        this.stationaryPosition = pathPoints.isEmpty() ? new Vec2(0, 0) : pathPoints.getFirst();
        this.distanceIntoLap = 0;
        this.x = 0;
        this.y = 0;
        // Rescaled the same way speed was (700f -> 700f*0.032 = 22.4f) so spawn timing is
        // unchanged now that speed is a direct px/tick value rather than a 0-999-per-segment
        // fixed-point unit.
        this.delay = Math.round(22.4f * delay / this.speed);
        if (delay == 0) {
            this.inactive = false;
            this.validTarget = true;
        }
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
        }
        if (this.health <= 0) {
            this.validTarget = false;
            this.dead = true;
            this.gameWorld.apply(EconomyDelta.kill(this.price));
            this.gameWorld.removeEnemy();
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
     * landing between two ticks. Equal to getX()/getY() at spawn and immediately after a
     * path-end wrap, where there is nothing meaningful to interpolate from.
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
     * {@link DefinedEnemyMob#doDamage} recomputes from its traits' {@code speedFactor} and
     * {@link #doInit} reads for the spawn delay - so a slow or freeze never gets permanently
     * baked into it, and never gets wiped out the next time a trait recomputes it.
     */
    private float effectiveSpeed() {
        return this.speed * this.activeEffects.speedMultiplier();
    }

    public float getSpeed() {
        return this.effectiveSpeed();
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

    /** The exact, sub-pixel distance into the path's current lap - see {@link #jumpToDistance}. */
    protected double getDistanceIntoLap() {
        return this.distanceIntoLap;
    }

    /**
     * Places this mob at an arbitrary point along the path instead of the start every mob
     * otherwise spawns at - what an ability-driven spawn (the Warden's egg, a reinforcement)
     * uses to appear where the spawning mob actually was, via
     * {@code DefinedEnemyMob.spawnAtSamePositionAs}. Must be called after {@link #doInit}.
     */
    protected void jumpToDistance(double distanceIntoLap) {
        this.distanceIntoLap = distanceIntoLap;
        this.updatePosition();
        this.prevX = this.x;
        this.prevY = this.y;
    }

    /** Spawned, on the board, and still alive - the precondition every targeting query applies. */
    public boolean validTarget() {
        return ((!this.inactive) && this.validTarget && (!this.dead));
    }

    public boolean validTarget(type type) {
        return (this.validTarget() && (type.equals(this.type)));
    }

    public boolean validTarget(type type1, type type2) {
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
     * arcLengthPath/distanceIntoLap state. A degenerate path (see doInit) has nothing to
     * measure distance along, so it just holds at its one available point.
     */
    private void updatePosition() {
        if (this.arcLengthPath != null) {
            PathPose pose = this.arcLengthPath.poseAt(this.distanceIntoLap);
            this.x = pose.position().x();
            this.y = pose.position().y();
            this.lastFacingRadians = pose.facingRadians();
        } else {
            this.x = this.stationaryPosition.x();
            this.y = this.stationaryPosition.y();
        }
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
            boolean wrappedToPathStart = false;
            if (this.arcLengthPath != null) {
                this.distanceIntoLap += this.speed * speedMultiplier;
                double totalLength = this.arcLengthPath.totalLength();
                if (this.distanceIntoLap >= totalLength) {
                    this.distanceIntoLap -= totalLength;
                    wrappedToPathStart = true;
                    this.gameWorld.apply(EconomyDelta.leak(this.price == 0 ? 10 : this.price));
                }
            }
            this.updatePosition();
            this.validTarget = this.x >= 0 && this.x <= this.gameWorld.getBoard().maxX() && this.y >= 0 && this.y <= this.gameWorld.getBoard().maxY();
            if (wrappedToPathStart) {
                // Reappearing at the path's start is a genuine teleport, not motion along
                // it - interpolating from the old (near path-end) position would draw a
                // streak clear across the board for one frame.
                this.prevX = this.x;
                this.prevY = this.y;
            }
        }
    }
}
