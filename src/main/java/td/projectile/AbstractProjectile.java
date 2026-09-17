package td.projectile;


import td.util.ThreadConfined;
/**
 * Everything every projectile shares: its current and previous-tick position (for the
 * renderer's interpolation, exactly like {@code AbstractEnemyMob}'s prevX/prevY pair), and
 * the finished/live lifecycle a {@code ProjectileRoster} drops it from once it resolves.
 * Subclasses supply only how one tick's worth of flight is resolved.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)  // position and lifetime, advanced by doTick
public abstract class AbstractProjectile implements Projectile {

    protected double x;
    protected double y;
    private double prevX;
    private double prevY;
    private boolean finished = false;

    protected AbstractProjectile(double startX, double startY) {
        this.x = startX;
        this.y = startY;
        this.prevX = startX;
        this.prevY = startY;
    }

    @Override
    public final void doTick(int gameTime) {
        if (this.finished) {
            return;
        }
        this.prevX = this.x;
        this.prevY = this.y;
        this.advance(gameTime);
    }

    /** Moves this projectile one tick's worth of flight, calling {@link #finish()} once it resolves. */
    protected abstract void advance(int gameTime);

    protected void finish() {
        this.finished = true;
    }

    @Override
    public boolean isFinished() {
        return this.finished;
    }

    @Override
    public double getX() {
        return this.x;
    }

    @Override
    public double getY() {
        return this.y;
    }

    @Override
    public double getPrevX() {
        return this.prevX;
    }

    @Override
    public double getPrevY() {
        return this.prevY;
    }
}
