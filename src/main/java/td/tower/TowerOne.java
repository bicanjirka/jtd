package td.tower;

import td.damage.Damage;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.targeting.FurthestAlongPathSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.upgrade.KillCountCondition;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradePath;
import td.util.GameWorld;

import java.util.List;

/**
 * "Triangle tower" - the cheap single-target one. Fires at whichever visible enemy in range
 * is furthest along the path, which is the usual right answer since that enemy is closest to
 * costing a life. Its turret head sweeps toward the target at a capped rate rather than
 * snapping, and holds its last heading when it has no target (see {@link TurretAim}).
 */
public final class TowerOne extends AbstractTower {

    public static final int price = 10;
    public static final int damage = 4000;
    public static final float range = 3.8f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.4;

    /** More damage and range, plus a bounty top-up on this tower's own kills - a veteran's payoff for proven kills. */
    private static final UpgradePath VETERAN = new UpgradePath(
            "Veteran", 30, new TowerBuff(0.3f, 0.1f, 0f, 0.25f), new KillCountCondition(10));
    /** Faster, weaker shots - a straightforward money-gated specialization needing no track record. */
    private static final UpgradePath OVERCLOCK = new UpgradePath(
            "Overclock", 25, new TowerBuff(-0.2f, 0f, 0.4f, 0f), UpgradeCondition.always());
    private static final List<UpgradePath> PATHS = List.of(VETERAN, OVERCLOCK);

    private int coolDown = 0;

    private EnemyMob currentTarget;
    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);

    public TowerOne(GameWorld context, int x, int y) {
        super(TowerFactory.type.first, price, damage, range);
        this.coolDownMax = 39;
        this.coolDownCurrent = this.coolDownMax;
        this.doInit(context, x, y);
    }

    @Override
    public List<UpgradePath> availablePaths() {
        return PATHS;
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
                this.dealDamage(this.currentTarget, Damage.physical(this.damageCurrent));
                this.coolDown = this.coolDownCurrent;
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
        return (float) this.coolDown / this.coolDownCurrent;
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
