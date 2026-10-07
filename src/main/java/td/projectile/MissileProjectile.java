package td.projectile;

import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.NearestSelector;
import td.util.ThreadConfined;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Homes on a live target, re-aiming each tick, hidden or not: a target that turns invisible does not
 * shake it off. If the target stops being valid it retargets to the visible enemy nearest the
 * missile.
 * <p>
 * Gives up when no target is left, and after a maximum lifetime in case it can never catch one. It
 * remembers where it has been lately, for the smoke it draws.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class MissileProjectile extends AbstractProjectile {

    private static final int MAX_LIFETIME_TICKS = 400;
    /** How many past positions a missile remembers. */
    private static final int TRAIL_LENGTH = 6;

    private final EnemyRegistry enemies;
    private final ProjectileStats stats;
    private final TargetImpact impact;
    private final Deque<TrailPoint> trail = new ArrayDeque<>();
    private EnemyMob target;
    private int ticksAlive = 0;

    public MissileProjectile(double startX, double startY, EnemyMob initialTarget, EnemyRegistry enemies,
                             ProjectileStats stats, TargetImpact impact) {
        super(startX, startY);
        this.target = initialTarget;
        this.enemies = enemies;
        this.stats = stats;
        this.impact = impact;
    }

    @Override
    protected void advance(int gameTime) {
        this.ticksAlive++;
        if (this.target == null || !this.target.validTarget()) {
            this.target = retarget();
        }
        if (this.target == null || this.ticksAlive > MAX_LIFETIME_TICKS) {
            this.finish();
            return;
        }
        this.remember();
        double speed = this.stats.speed();
        double dx = this.target.getX() - this.x;
        double dy = this.target.getY() - this.y;
        double distance = Math.hypot(dx, dy);
        if (distance <= speed) {
            this.x = this.target.getX();
            this.y = this.target.getY();
            this.impact.onImpact(this.target);
            this.finish();
        } else {
            this.x += dx / distance * speed;
            this.y += dy / distance * speed;
        }
    }

    private void remember() {
        if (this.trail.size() == TRAIL_LENGTH) {
            this.trail.removeFirst();
        }
        this.trail.addLast(new TrailPoint(this.x, this.y));
    }

    private EnemyMob retarget() {
        List<EnemyMob> candidates = InRangeTargetQuery
                .visible((int) Math.round(this.x), (int) Math.round(this.y), Float.MAX_VALUE)
                .matching(this.enemies);
        return new NearestSelector(this.x, this.y).selectFrom(candidates).orElse(null);
    }

    public ProjectileStats stats() {
        return this.stats;
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
