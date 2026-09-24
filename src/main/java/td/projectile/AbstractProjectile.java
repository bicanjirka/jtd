package td.projectile;


import td.util.ThreadConfined;

/**
 * What every projectile shares: current and previous-tick position for interpolation, and the
 * finished flag that gets it dropped from the roster. Subclasses supply one tick of flight.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
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

    /** Moves one tick, calling {@link #finish()} once resolved. */
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
