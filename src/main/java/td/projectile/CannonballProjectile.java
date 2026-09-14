package td.projectile;

/**
 * Travels in a straight line, at a fixed speed, to the destination it was aimed at when
 * fired - it never re-aims in flight, so a fast-moving enemy can dodge it by the time it
 * arrives. Detonates at that destination whether or not anything is still there to hit; a
 * fixed destination is always reached in finite ticks, so this needs no lifetime cap the way
 * a homing {@link MissileProjectile} does.
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
