package td.enemy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.util.Context;
import td.wave.Path;
import td.wave.Point;

public abstract class AbstractEnemyMob implements EnemyMob, Cloneable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractEnemyMob.class);

    protected type type;
    protected boolean inactive = true;
    protected boolean validTarget = false;
    protected boolean dead = false;
    protected int price;
    protected int level;
    protected int x, y;
    private int prevX, prevY;
    protected int speed = 40;
    protected int speedMax = 40;
    protected final int speedBase = 40;
    protected int health;
    protected int healthMax;
    protected Context context;
    private int delay;
    private int deathTick = -1;
    private Path path;
    private int segment = 0;
    private int segmentProgression = 0;
    private Point segmentStartPoint;
    private Point segmentEndPoint;


    public AbstractEnemyMob() {
        this.type = EnemyMob.type.Normal;
    }

    protected void doInit(Context context, int delay, int health, int price, int level) {
        this.context = context;
        this.price = price;
        this.level = level;
        this.health = health * 100;
        this.healthMax = health * 100;
        this.path = this.context.getPath();
        this.x = 0;
        this.y = 0;
        this.delay = Math.round(700f * delay / this.speed);
        if (delay == 0) {
            this.inactive = false;
            this.validTarget = true;
        }
        this.resetPosition();
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

    public void doDamage(int damage) {
        if (this.validTarget()) {
            this.health -= damage;
        }
        if (this.health <= 0) {
            this.validTarget = false;
            this.dead = true;
            this.context.addScore(this.price);
            this.context.doReceive(this.price);
            this.context.removeEnemy();
        }
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    /**
     * This mob's x/y as of the tick before last, i.e. the interpolation source for a render
     * landing between two ticks. Equal to getX()/getY() at spawn and immediately after a
     * path-end wrap, where there is nothing meaningful to interpolate from.
     */
    public int getPrevX() {
        return this.prevX;
    }

    public int getPrevY() {
        return this.prevY;
    }

    public int getSpeed() {
        return this.speed;
    }

    public int getProgression() {
        return 1000 * this.segment + this.segmentProgression;
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

    private void resetPosition() {
        this.segmentStartPoint = this.path.getStep(this.segment);
        this.segmentEndPoint = this.path.getStep(this.segment + 1);
        this.x = this.segmentStartPoint.x() + (this.segmentEndPoint.x() - this.segmentStartPoint.x()) * this.segmentProgression / 1000;
        this.y = this.segmentStartPoint.y() + (this.segmentEndPoint.y() - this.segmentStartPoint.y()) * this.segmentProgression / 1000;
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
            this.segmentProgression += this.speed;
            if (this.segmentProgression >= 1000) {
                this.segmentProgression -= 1000;
                this.segment++;
                if (this.segment >= this.path.length()) {
                    this.segment = 0;
                    wrappedToPathStart = true;
                    if (this.price == 0) this.context.deductScore(10);
                    else this.context.deductScore(this.price);
                    this.context.removeLife();
                }
                this.segmentStartPoint = this.path.getStep(this.segment);
                this.segmentEndPoint = this.path.getStep(this.segment + 1);
            }
            this.x = this.segmentStartPoint.x() + (this.segmentEndPoint.x() - this.segmentStartPoint.x()) * this.segmentProgression / 1000;
            this.y = this.segmentStartPoint.y() + (this.segmentEndPoint.y() - this.segmentStartPoint.y()) * this.segmentProgression / 1000;
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
