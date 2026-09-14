package td.tower;

import td.damage.Damage;
import td.effect.Effect;
import td.enemy.EnemyMob;
import td.projectile.MissileProjectile;
import td.tower.targeting.FurthestAlongPathSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.util.GameWorld;

import java.util.List;

/**
 * "Seeker tower" - fires a homing missile at whichever visible enemy is furthest along the
 * path, the same target choice as {@link TowerOne}. Unlike {@link TowerMortar}'s shell, the
 * missile re-aims each tick at its target's live position and retargets to the nearest
 * remaining enemy if that target dies or leaks before it arrives (see
 * {@code MissileProjectile}). On impact it deals magic damage and freezes whichever mob it
 * actually reached - which may not be the one it was originally fired at.
 */
public final class TowerSeeker extends AbstractTower {

    public static final int price = 35;
    public static final int damage = 1800;
    public static final float range = 4.5f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.3;
    private static final float PROJECTILE_SPEED = 35f;
    private static final int FREEZE_DURATION_TICKS = 30;

    private int coolDown = 0;
    private EnemyMob currentTarget;
    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);

    public TowerSeeker(GameWorld context, int x, int y) {
        super(TowerFactory.type.seeker, price, damage, range);
        this.coolDownMax = 60;
        this.coolDownCurrent = this.coolDownMax;
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
        this.context.addProjectile(new MissileProjectile(this.centerX, this.centerY, target,
                this.context.getEnemyRegistry(), PROJECTILE_SPEED, this::onImpact));
    }

    private void onImpact(EnemyMob target) {
        this.dealDamage(target, Damage.magic(this.damageCurrent));
        target.applyEffect(Effect.freeze(FREEZE_DURATION_TICKS, d -> this.dealDamage(target, d)));
    }

    public EnemyMob getCurrentTarget() {
        return this.currentTarget;
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitTowerSeeker(this);
    }

    public String getInfoString() {
        return "Seeker tower\n\n" +
                super.getInfoString() +
                "Fires a homing missile\n" +
                "Freezes what it hits";
    }

    public String getStatusString() {
        return "Seeker tower\n\n" +
                super.getStatusString() +
                "Fires a homing missile\n" +
                "Freezes what it hits";
    }
}
