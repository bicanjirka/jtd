package td.projectile;

/**
 * Flies straight to the point it was aimed at and never re-aims, so a fast enemy can dodge it.
 * Detonates there whether or not anything is left to hit.
 */
public final class CannonballProjectile extends AbstractProjectile {

    private final double destinationX;
    private final double destinationY;
    private final ProjectileStats stats;
    private final ShellLook look;
    private final PointImpact impact;

    public CannonballProjectile(double startX, double startY, double destinationX, double destinationY,
                                float speed, PointImpact impact) {
        this(startX, startY, destinationX, destinationY, ProjectileStats.of(speed), ShellLook.PLAIN, impact);
    }

    public CannonballProjectile(double startX, double startY, double destinationX, double destinationY,
                                ProjectileStats stats, ShellLook look, PointImpact impact) {
        super(startX, startY);
        this.destinationX = destinationX;
        this.destinationY = destinationY;
        this.stats = stats;
        this.look = look;
        this.impact = impact;
    }

    public ProjectileStats stats() {
        return this.stats;
    }

    /** What this shell carries. */
    public ShellLook look() {
        return this.look;
    }

    @Override
    protected void advance(int gameTime) {
        double dx = this.destinationX - this.x;
        double dy = this.destinationY - this.y;
        double distance = Math.hypot(dx, dy);
        float speed = this.stats.speed();
        if (distance <= speed) {
            this.x = this.destinationX;
            this.y = this.destinationY;
            this.impact.onImpact(this.destinationX, this.destinationY);
            this.finish();
        } else {
            this.x += dx / distance * speed;
            this.y += dy / distance * speed;
        }
    }

    @Override
    public <R> R accept(ProjectileVisitor<R> visitor) {
        return visitor.visitCannonball(this);
    }
}
