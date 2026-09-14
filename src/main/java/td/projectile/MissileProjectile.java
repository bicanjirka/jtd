package td.projectile;

import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.NearestSelector;

import java.util.List;

/**
 * Homes on a live target, re-aiming each tick at its current position, until it reaches it or
 * the target is no longer valid - in which case it retargets to the nearest remaining enemy
 * (centred on the missile's own current position, not the tower that fired it) rather than
 * fizzling. Only a {@link EnemyMob.type#Normal} enemy is ever targeted or retargeted onto,
 * matching every other single-target tower's convention.
 * <p>
 * A missile that can find no valid target anywhere gives up rather than flying forever; a max
 * lifetime is a second, independent safety net against a homing edge case (e.g. a target it
 * can never quite catch) doing the same.
 */
public final class MissileProjectile extends AbstractProjectile {

    private static final int MAX_LIFETIME_TICKS = 400;

    private final EnemyRegistry enemies;
    private final float speed;
    private final TargetImpact impact;
    private EnemyMob target;
    private int ticksAlive = 0;

    public MissileProjectile(double startX, double startY, EnemyMob initialTarget, EnemyRegistry enemies,
                              float speed, TargetImpact impact) {
        super(startX, startY);
        this.target = initialTarget;
        this.enemies = enemies;
        this.speed = speed;
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
        double dx = this.target.getX() - this.x;
        double dy = this.target.getY() - this.y;
        double distance = Math.hypot(dx, dy);
        if (distance <= this.speed) {
            this.x = this.target.getX();
            this.y = this.target.getY();
            this.impact.onImpact(this.target);
            this.finish();
        } else {
            this.x += dx / distance * this.speed;
            this.y += dy / distance * this.speed;
        }
    }

    private EnemyMob retarget() {
        List<EnemyMob> candidates = InRangeTargetQuery
                .ofType((int) Math.round(this.x), (int) Math.round(this.y), Float.MAX_VALUE, EnemyMob.type.Normal)
                .matching(this.enemies);
        return new NearestSelector(this.x, this.y).selectFrom(candidates).orElse(null);
    }

    @Override
    public <R> R accept(ProjectileVisitor<R> visitor) {
        return visitor.visitMissile(this);
    }
}
