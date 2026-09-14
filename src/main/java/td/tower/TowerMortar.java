package td.tower;

import td.damage.Damage;
import td.effect.Effect;
import td.enemy.EnemyMob;
import td.projectile.CannonballProjectile;
import td.tower.targeting.FurthestAlongPathSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.util.GameWorld;

import java.util.List;

/**
 * "Mortar tower" - lobs a slow, unguided shell at whichever visible enemy is furthest along
 * the path, the same target choice as {@link TowerOne}. The shell travels in a straight line
 * to that enemy's position at the moment of firing and never re-aims, so a fast-moving enemy
 * can dodge it by the time it lands (see {@code CannonballProjectile}); on arrival it splashes
 * physical damage with the same distance-falloff shape {@link TowerTwo} uses, and slows every
 * enemy the blast reaches.
 */
public final class TowerMortar extends AbstractTower {

    public static final int price = 30;
    public static final int damage = 2000;
    public static final float range = 4.0f;
    public static final float splashRadiusBase = 2.0f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.3;
    private static final float PROJECTILE_SPEED = 40f;
    private static final float SLOW_MULTIPLIER = 0.5f;
    private static final int SLOW_DURATION_TICKS = 40;

    private final float splashRadius;
    private int coolDown = 0;
    private EnemyMob currentTarget;
    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);

    public TowerMortar(GameWorld context, int x, int y) {
        super(TowerFactory.type.mortar, price, damage, range);
        this.coolDownMax = 50;
        this.coolDownCurrent = this.coolDownMax;
        this.splashRadius = splashRadiusBase * context.getBoard().scale();
        this.doInit(context, x, y);
    }

    private EnemyMob findTarget() {
        List<EnemyMob> inRange = InRangeTargetQuery.ofType(this.centerX, this.centerY, this.rangeReal, EnemyMob.type.Normal)
                .matching(this.context.getEnemyRegistry());
        return new FurthestAlongPathSelector().selectFrom(inRange).orElse(null);
    }

    public void doTick(int gameTime) {
        if (this.coolDown > 0) {
            this.coolDown--;
        } else {
            this.currentTarget = this.findTarget();
            if (this.currentTarget != null) {
                this.fireAt(this.currentTarget);
                this.coolDown = this.coolDownCurrent;
            }
        }
        // No target: hold the last heading rather than snapping back to a neutral angle - see
        // TurretAim's class doc on skipping tick() while idle.
        if (this.currentTarget != null) {
            this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, this.currentTarget.getX(), this.currentTarget.getY()));
        }
    }

    private void fireAt(EnemyMob target) {
        this.context.addProjectile(new CannonballProjectile(this.centerX, this.centerY, target.getX(), target.getY(),
                PROJECTILE_SPEED, this::onImpact));
    }

    private void onImpact(double x, double y) {
        List<EnemyMob> hit = InRangeTargetQuery.anyType((int) Math.round(x), (int) Math.round(y), this.splashRadius)
                .matching(this.context.getEnemyRegistry());
        for (EnemyMob enemy : hit) {
            double dx = x - enemy.getX();
            double dy = y - enemy.getY();
            float r2 = (float) (dx * dx + dy * dy);
            int amount = Math.round(this.damageCurrent * (1 - r2 / (this.splashRadius * this.splashRadius)));
            this.dealDamage(enemy, Damage.physical(amount));
            enemy.applyEffect(Effect.slow(SLOW_MULTIPLIER, SLOW_DURATION_TICKS, d -> this.dealDamage(enemy, d)));
        }
    }

    public EnemyMob getCurrentTarget() {
        return this.currentTarget;
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitTowerMortar(this);
    }

    public String getInfoString() {
        return "Mortar tower\n\n" +
                super.getInfoString() +
                "Lobs a slow, unguided shell\n" +
                "Splashes and slows on impact";
    }

    public String getStatusString() {
        return "Mortar tower\n\n" +
                super.getStatusString() +
                "Lobs a slow, unguided shell\n" +
                "Splashes and slows on impact";
    }
}
