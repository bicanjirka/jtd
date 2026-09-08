package td.tower;

import td.damage.Damage;
import td.enemy.EnemyMob;
import td.tower.targeting.FurthestAlongPathSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.util.GameWorld;

import java.util.List;

public final class TowerOne extends AbstractTower {

    public static final int price = 10;
    public static final int damage = 4000;
    public static final float range = 3.8f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.4;

    private int coolDown = 0;

    private EnemyMob currentTarget;
    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);

    public TowerOne(GameWorld context, int x, int y) {
        super(TowerFactory.type.first, price, damage, range);
        this.coolDownMax = 39;
        this.doInit(context, x, y);
    }

    private EnemyMob findEnemy() {
        List<EnemyMob> inRange = InRangeTargetQuery.ofType(this.centerX, this.centerY, this.rangeReal, EnemyMob.type.Normal)
                .matching(this.context.getEnemyRegistry());
        return new FurthestAlongPathSelector().selectFrom(inRange).orElse(null);
    }

    public void doTick(int gameTime) {
        if (this.coolDown > 0) {
            this.coolDown--;
        } else {
            this.currentTarget = this.findEnemy();
            if (this.currentTarget != null) {
                this.dealDamage(this.currentTarget, Damage.of(this.damageCurrent));
                this.coolDown = this.coolDownMax;
            }
        }
        // No target: hold the last heading rather than snapping back to a neutral angle - see
        // TurretAim's class doc on skipping tick() while idle.
        if (this.currentTarget != null) {
            this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, this.currentTarget.getX(), this.currentTarget.getY()));
        }
    }

    public EnemyMob getCurrentTarget() {
        return this.currentTarget;
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    public float getCoolDownFraction() {
        return (float) this.coolDown / this.coolDownMax;
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitTowerOne(this);
    }

    public String getInfoString() {
        return "Triangle tower\n\n" +
                super.getInfoString() +
                "Targets first one";
    }

    public String getStatusString() {
        return "Triangle tower\n\n" +
                super.getStatusString() +
                "Targets first one";
    }

}
