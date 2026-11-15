package td.projectile;

import td.enemy.EnemyMob;
import td.util.ThreadConfined;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Flies like an aircraft at a live target, hidden or not: it leaves on the heading it was launched with, speeds up
 * gradually to its top speed and turns toward its target on a smooth curve, harder the nearer it gets, so it never
 * circles even a fast enemy and always reaches it. Its {@link MissileGuide} says when to give a target up and whom to
 * turn to; with no one to hunt it flies back to where it was launched.
 * <p>
 * Gives up after a maximum lifetime in case it can never catch one. It remembers where it has been lately, for the
 * smoke it draws.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class MissileProjectile extends AbstractProjectile {

    private static final int MAX_LIFETIME_TICKS = 400;
    /** How many past positions a missile remembers. */
    private static final int TRAIL_LENGTH = 6;
    /** The share of its top speed it leaves with. */
    private static final double LAUNCH_SPEED_SHARE = 0.35;
    /** The share of its top speed it gains each tick until it flies at it. */
    private static final double SPEED_GAIN_SHARE = 0.065;
    /** How fast it turns when far from its target, in radians a tick. */
    private static final double FAR_TURN_RADIANS = 0.15;
    /**
     * Near its target it turns at this many times its speed over the distance left: a turning circle under half that
     * distance, so it closes in on a curve rather than orbiting.
     */
    private static final double CLOSING_TURN = 2.5;

    private final double homeX;
    private final double homeY;
    private final ProjectileStats stats;
    private final MissileGuide guide;
    private final Deque<TrailPoint> trail = new ArrayDeque<>();
    private Optional<EnemyMob> target;
    private double heading;
    private double speed;
    private int ticksAlive = 0;

    /** A missile leaving ({@code startX}, {@code startY}) on {@code headingRadians}, after {@code target}. */
    public MissileProjectile(double startX, double startY, double headingRadians, EnemyMob target,
                             ProjectileStats stats, MissileGuide guide) {
        super(startX, startY);
        this.homeX = startX;
        this.homeY = startY;
        this.heading = headingRadians;
        this.target = Optional.of(target);
        this.stats = stats;
        this.guide = guide;
        this.speed = stats.speed() * LAUNCH_SPEED_SHARE;
    }

    @Override
    protected void advance(int gameTime) {
        this.ticksAlive++;
        if (this.ticksAlive > MAX_LIFETIME_TICKS) {
            this.finish();
            return;
        }
        if (this.target.isEmpty() || !this.target.get().validTarget() || !this.guide.keeps(this.target.get())) {
            this.target = this.guide.retarget(this.x, this.y);
        }
        this.remember();
        this.speed = Math.min(this.stats.speed(), this.speed + this.stats.speed() * SPEED_GAIN_SHARE);
        double toX = this.target.map(EnemyMob::getX).orElse(this.homeX);
        double toY = this.target.map(EnemyMob::getY).orElse(this.homeY);
        double distance = Math.hypot(toX - this.x, toY - this.y);
        if (distance <= this.speed) {
            this.x = toX;
            this.y = toY;
            this.target.ifPresentOrElse(this.guide::land, this.guide::arriveHome);
            this.finish();
            return;
        }
        this.turnToward(Math.atan2(toY - this.y, toX - this.x), distance);
        this.x += Math.cos(this.heading) * this.speed;
        this.y += Math.sin(this.heading) * this.speed;
    }

    /** Turns toward {@code wanted}, no further this tick than it can turn at {@code distance} from its target. */
    private void turnToward(double wanted, double distance) {
        double most = Math.min(Math.PI, Math.max(FAR_TURN_RADIANS, CLOSING_TURN * this.speed / distance));
        double turn = Math.IEEEremainder(wanted - this.heading, 2 * Math.PI);
        this.heading += Math.clamp(turn, -most, most);
    }

    private void remember() {
        if (this.trail.size() == TRAIL_LENGTH) {
            this.trail.removeFirst();
        }
        this.trail.addLast(new TrailPoint(this.x, this.y));
    }

    public ProjectileStats stats() {
        return this.stats;
    }

    /** Where it is heading, in radians. */
    public double headingRadians() {
        return this.heading;
    }

    /** How fast it flies now, in pixels a tick: below its top speed while it speeds up. */
    public double speed() {
        return this.speed;
    }

    /** Where it has been, oldest first; at most a few ticks' worth. */
    public List<TrailPoint> trail() {
        return List.copyOf(this.trail);
    }

    @Override
    public <R> R accept(ProjectileVisitor<R> visitor) {
        return visitor.visitMissile(this);
    }

    /** A position the missile passed through. */
    public record TrailPoint(double x, double y) {
    }
}
