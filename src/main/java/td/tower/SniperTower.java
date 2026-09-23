package td.tower;

import td.damage.Damage;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.targeting.FurthestAlongPathSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.upgrade.KillCountCondition;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.tower.upgrade.UpgradeTree;
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
    public static final int DAMAGE = 3000;
    public static final float RANGE = 3.8f;
    public static final float CRIT_CHANCE = 0.15f;
    private static final double MAX_TURN_RADIANS_PER_TICK = 0.4;

    /**
     * More damage and range, a bounty top-up on this tower's own kills, and a further boost to
     * this tower's own crit chance - a veteran marksman's proven aim starts placing shots that
     * count extra even more often.
     */
    private static final UpgradeNode VETERAN = UpgradeNode.of("sniper.head.veteran", UpgradeSlot.HEAD, "Veteran", 30)
            .withBuff(TowerBuff.damage(0.3f).withRange(0.1f).withBounty(0.25f).withCritChance(0.3f))
            .withRequires(UpgradeCondition.slotEmpty(UpgradeSlot.HEAD))
            .withGate(new KillCountCondition(10));
    /**
     * Faster, weaker shots - a straightforward money-gated specialization needing no track record.
     */
    private static final UpgradeNode OVERCLOCK = UpgradeNode.of("sniper.head.overclock", UpgradeSlot.HEAD,
            "Overclock", 25)
            .withBuff(TowerBuff.damage(-0.2f).withFireRate(0.4f))
            .withRequires(UpgradeCondition.slotEmpty(UpgradeSlot.HEAD));
    private static final UpgradeTree TREE = UpgradeTree.of(VETERAN, OVERCLOCK);

    /**
     * Ticks between shots before any fire-rate buff.
     */
    private static final int COOLDOWN_MAX = 39;
    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private int coolDown = 0;
    private EnemyMob currentTarget;
    private boolean lastShotCritical;

    public SniperTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SNIPER, PRICE,
                new TowerBaseStats(DAMAGE, RANGE, COOLDOWN_MAX).withCritChance(CRIT_CHANCE), context, x, y);
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
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
                this.lastShotCritical = this.dealDamage(this.currentTarget, Damage.physical(this.damageCurrent()));
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

    public boolean wasLastShotCritical() {
        return this.lastShotCritical;
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
