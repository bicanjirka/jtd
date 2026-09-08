package td.enemy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.damage.Damage;
import td.economy.EconomyDelta;
import td.util.Context;
import td.wave.ArcLengthPath;
import td.wave.PathPose;
import td.wave.Point;
import td.wave.Vec2;

import java.util.Optional;

public abstract class AbstractEnemyMob implements EnemyMob, Cloneable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractEnemyMob.class);

    // Rescaled from the old fixed-point model (speed=40 meant "40/1000 of the current
    // segment per tick", which - since every segment was exactly one 32px cell - worked out
    // to 40/1000*32 = 1.28 px/tick). speed is now pixels per tick directly, so every existing
    // enemy's actual speed is unchanged; only the unit it's expressed in is.
    protected type type;
    protected boolean inactive = true;
    protected boolean validTarget = false;
    protected boolean dead = false;
    protected int price;
    protected int level;
    protected double x, y;
    private double prevX, prevY;
    protected float speed = 1.28f;
    protected float speedMax = 1.28f;
    protected final float speedBase = 1.28f;
    protected int health;
    protected int healthMax;
    protected Context context;
    private int delay;
    private int deathTick = -1;
    private ArcLengthPath arcLengthPath;
    private Vec2 stationaryPosition;
    private double distanceIntoLap = 0;
    private double lastFacingRadians = 0;


    public AbstractEnemyMob() {
        this.type = EnemyMob.type.Normal;
    }

    protected void doInit(Context context, int delay, int health, int price, int level) {
        this.context = context;
        this.price = price;
        this.level = level;
        this.health = health * 100;
        this.healthMax = health * 100;
        Optional<ArcLengthPath> arcLength = ArcLengthPath.of(this.context.getPath());
        this.arcLengthPath = arcLength.orElse(null);
        // A degenerate path (PathEmpty, or a not-yet-finalised path in a test) has nothing to
        // measure distance along - hold at its one available point rather than move at all.
        // Path.getStep still returns the int-pixel Point at this point in the refactor.
        Point firstStep = this.context.getPath().getStep(0);
        this.stationaryPosition = new Vec2(firstStep.x(), firstStep.y());
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

    public void doDamage(Damage damage) {
        if (this.dead) {
            return;
        }
        if (this.validTarget()) {
            this.health -= this.absorb(damage).amount();
        }
        if (this.health <= 0) {
            this.validTarget = false;
            this.dead = true;
            this.context.apply(EconomyDelta.kill(this.price));
            this.context.removeEnemy();
        }
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
            this.validTarget = this.x >= 0 && this.x <= this.context.maxX && this.y >= 0 && this.y <= this.context.maxY;
            if (wrappedToPathStart) {
                // Reappearing at the path's start is a genuine teleport, not motion along
                // it - interpolating from the old (near path-end) position would draw a
                // streak clear across the board for one frame.
                this.prevX = this.x;
                this.prevY = this.y;
            }
        }
    }

    public EnemyMob create(Context context, int delay, int health, int price, int level) {
        AbstractEnemyMob newEnemy;
        try {
            newEnemy = (AbstractEnemyMob) this.clone();
        } catch (CloneNotSupportedException ex) {
            LOG.error("Failed to clone enemy prototype {}", this.getClass().getSimpleName(), ex);
            throw new IllegalStateException("Failed to clone enemy prototype " + this.getClass().getSimpleName(), ex);
        }
        newEnemy.doInit(context, delay, health, price, level);

        return newEnemy;
    }
}
