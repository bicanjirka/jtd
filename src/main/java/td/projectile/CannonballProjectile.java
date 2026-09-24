package td.projectile;

/**
 * Flies straight to the point it was aimed at and never re-aims, so a fast enemy can dodge it.
 * Detonates there whether or not anything is left to hit.
 */
public final class CannonballProjectile extends AbstractProjectile {

    private final double destinationX;
    private final double destinationY;
    private final float speed;
    private final PointImpact impact;

    public CannonballProjectile(double startX, double startY, double destinationX, double destinationY,
                                float speed, PointImpact impact) {
        super(startX, startY);
        this.destinationX = destinationX;
        this.destinationY = destinationY;
        this.speed = speed;
        this.impact = impact;
    }

    @Override
    protected void advance(int gameTime) {
        double dx = this.destinationX - this.x;
        double dy = this.destinationY - this.y;
        double distance = Math.hypot(dx, dy);
        if (distance <= this.speed) {
            this.x = this.destinationX;
            this.y = this.destinationY;
            this.impact.onImpact(this.destinationX, this.destinationY);
            this.finish();
        } else {
            this.x += dx / distance * this.speed;
            this.y += dy / distance * this.speed;
        }
    }

    @Override
    public <R> R accept(ProjectileVisitor<R> visitor) {
        return visitor.visitCannonball(this);
    }
}
