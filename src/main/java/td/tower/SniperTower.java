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
import td.util.ThreadConfined;

import java.util.List;

/**
 * "Sniper tower" - the cheap single-target one. Fires at whichever visible enemy in range
 * is furthest along the path, which is the usual right answer since that enemy is closest to
 * costing a life. Its turret head sweeps toward the target at a capped rate rather than
 * snapping, and holds its last heading when it has no target (see {@link TurretAim}).
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)  // cooldown and current target, advanced by doTick
public final class SniperTower extends AbstractTower {

    public static final int PRICE = 10;
    public static final int DAMAGE = 4000;
    public static final float RANGE = 3.8f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.4;

    /**
     * More damage and range, plus a bounty top-up on this tower's own kills - a veteran's payoff for proven kills.
     */
    private static final UpgradePath VETERAN = new UpgradePath(
            "Veteran", 30, new TowerBuff(0.3f, 0.1f, 0f, 0.25f), new KillCountCondition(10));
    /**
     * Faster, weaker shots - a straightforward money-gated specialization needing no track record.
     */
    private static final UpgradePath OVERCLOCK = new UpgradePath(
            "Overclock", 25, new TowerBuff(-0.2f, 0f, 0.4f, 0f), UpgradeCondition.always());
    private static final List<UpgradePath> PATHS = List.of(VETERAN, OVERCLOCK);

    /**
     * Ticks between shots before any fire-rate buff.
     */
    private static final int COOLDOWN_MAX = 39;
    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private int coolDown = 0;
    private EnemyMob currentTarget;

    public SniperTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SNIPER, PRICE, DAMAGE, RANGE, COOLDOWN_MAX, context, x, y);
    }

    @Override
    public List<UpgradePath> availablePaths() {
        return PATHS;
    }

    private EnemyMob findEnemy() {
        List<EnemyMob> inRange = InRangeTargetQuery.ofType(this.centerX, this.centerY, this.rangeReal(), EnemyMob.Type.NORMAL)
                .matching(this.context.enemies());
        return new FurthestAlongPathSelector().selectFrom(inRange).orElse(null);
    }

    public void doTick(int gameTime) {
        if (this.coolDown > 0) {
            this.coolDown--;
        } else {
            this.currentTarget = this.findEnemy();
            if (this.currentTarget != null) {
                this.dealDamage(this.currentTarget, Damage.physical(this.damageCurrent()));
                this.coolDown = this.coolDownCurrent();
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
        return (float) this.coolDown / this.coolDownCurrent();
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSniperTower(this);
    }

    public String getInfoString() {
        return "Sniper tower\n\n" +
                super.getInfoString() +
                "Targets first one";
    }

    public String getStatusString() {
        return "Sniper tower\n\n" +
                super.getStatusString() +
                "Targets first one";
    }

}
