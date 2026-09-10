package td.enemy;

import td.damage.Damage;
import td.economy.EconomyDelta;
import td.util.GameWorld;
import td.wave.ArcLengthPath;
import td.wave.PathPose;
import td.wave.Vec2;

import java.util.List;
import java.util.Optional;

/**
 * Everything every enemy shares: spawn delay, movement along the level's path, health and
 * damage, and the death-fade animation's timing. Subclasses add a body scale, a facing angle,
 * and at most a small behavioural twist (see {@link #absorb} and {@code EnemyMobTriangle}'s
 * speed-up on damage).
 * <p>
 * Movement is real arc-length distance: {@link #doTick} advances {@code distanceIntoLap} by
 * {@code speed} pixels and resolves it through the shared {@link ArcLengthPath}, so a curved
 * or diagonal path moves a mob at the same real-world pace a straight one does. Reaching the
 * end wraps back to the start and charges the player a {@link EconomyDelta#leak} - a mob is
 * never removed from the roster by walking, only by dying.
 */
public abstract class AbstractEnemyMob implements EnemyMob {

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
    protected int health;
    protected int healthMax;
    protected GameWorld context;
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
    protected void doInit(GameWorld context, int delay, int health, int price, int level) {
        this.context = context;
        this.price = price;
        this.level = level;
        this.health = health * 100;
        this.healthMax = health * 100;
        Optional<ArcLengthPath> arcLength = ArcLengthPath.of(this.context.getPath());
        this.arcLengthPath = arcLength.orElse(null);
        // A degenerate path (fewer than two points - e.g. an empty placeholder GameWorld has
        // before any level loads) has nothing to measure distance along - hold at its one
        // available point (or the origin, if it has none at all) rather than move at all.
        List<Vec2> pathPoints = this.context.getPath().points();
        this.stationaryPosition = pathPoints.isEmpty() ? new Vec2(0, 0) : pathPoints.get(0);
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

    public long getHealth() {
        return this.health;
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
     * @return the damage that actually landed: {@link #absorb}'s result for a live, valid
     * target, and {@link Damage#none()} for a mob that is already dead or not currently
     * targetable. Not capped at the mob's remaining health - a killing blow reports its whole
     * landed amount, overkill included.
     */
    public Damage doDamage(Damage damage) {
        if (this.dead) {
            return Damage.none();
        }
        Damage landed = Damage.none();
        if (this.validTarget()) {
            landed = this.absorb(damage);
            this.health -= landed.amount();
        }
        if (this.health <= 0) {
            this.validTarget = false;
            this.dead = true;
            this.context.apply(EconomyDelta.kill(this.price));
            this.context.removeEnemy();
        }
        return landed;
    }

    /**
     * Hook for a mob that resists part of an incoming hit (see EnemyMobSquare). The default
     * is no resistance - the damage lands unchanged.
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

    public float getSpeed() {
        return this.speed;
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
     * not derived from a pixel delta over one tick (see {@link AbstractEnemyMobDirectional}).
     */
    protected double getPathFacingRadians() {
        return this.lastFacingRadians;
    }

    /**
     * Counts down the spawn delay, or advances a live mob along the path, or - once dead -
     * records the tick death happened on so the fade can be timed against the simulation
     * clock rather than the repaint rate.
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
            this.prevX = this.x;
            this.prevY = this.y;
            boolean wrappedToPathStart = false;
            if (this.arcLengthPath != null) {
                this.distanceIntoLap += this.speed;
                double totalLength = this.arcLengthPath.totalLength();
                if (this.distanceIntoLap >= totalLength) {
                    this.distanceIntoLap -= totalLength;
                    wrappedToPathStart = true;
                    this.context.apply(EconomyDelta.leak(this.price == 0 ? 10 : this.price));
                }
            }
            this.updatePosition();
            this.validTarget = this.x >= 0 && this.x <= this.context.getBoard().maxX() && this.y >= 0 && this.y <= this.context.getBoard().maxY();
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
