package td.tower;

import td.damage.Damage;
import td.effect.Effect;
import td.enemy.EnemyMob;
import td.projectile.MissileProjectile;
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
 * "Seeker tower" - fires a homing missile at whichever visible enemy is furthest along the
 * path, the same target choice as {@link SniperTower}. Unlike {@link MortarTower}'s shell, the
 * missile re-aims each tick at its target's live position and retargets to the nearest
 * remaining enemy if that target dies or leaks before it arrives (see
 * {@code MissileProjectile}). On impact it deals magic damage and freezes whichever mob it
 * actually reached - which may not be the one it was originally fired at.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)  // cooldown and current target, advanced by doTick
public final class SeekerTower extends AbstractTower {

    public static final int PRICE = 35;
    public static final int DAMAGE = 2600;
    public static final float RANGE = 4.5f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.3;
    private static final float PROJECTILE_SPEED = 35f;
    private static final int FREEZE_DURATION_TICKS_BASE = 30;
    private static final float DEEP_FREEZE_DURATION_MULTIPLIER = 1.5f;

    /**
     * Faster reloading - a straightforward money-gated specialization needing no track record.
     */
    private static final UpgradeNode TWIN_WARHEAD = UpgradeNode.of("seeker.head.twin_warhead", UpgradeSlot.HEAD,
            "Twin Warhead", 30)
            .withBuff(TowerBuff.fireRate(0.35f))
            .withRequires(UpgradeCondition.slotEmpty(UpgradeSlot.HEAD));
    /**
     * More damage and a longer freeze - earned by this tower having racked up proven kills.
     */
    private static final UpgradeNode DEEP_FREEZE = UpgradeNode.of("seeker.head.deep_freeze", UpgradeSlot.HEAD,
            "Deep Freeze", 35)
            .withBuff(TowerBuff.damage(0.3f))
            .withRequires(UpgradeCondition.slotEmpty(UpgradeSlot.HEAD))
            .withGate(new KillCountCondition(10))
            .withExtraEffect("+50% freeze duration");
    private static final UpgradeTree TREE = UpgradeTree.of(TWIN_WARHEAD, DEEP_FREEZE);

    /**
     * Ticks between shots before any fire-rate buff - paired with this tower's damage.
     */
    private static final int COOLDOWN_MAX = 45;

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private volatile int freezeDurationTicks = FREEZE_DURATION_TICKS_BASE;
    private int coolDown = 0;
    private EnemyMob currentTarget;

    public SeekerTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SEEKER, PRICE, new TowerBaseStats(DAMAGE, RANGE, COOLDOWN_MAX), context, x, y);
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    /**
     * Deep Freeze's longer duration isn't a {@link TowerBuff} axis, so it's applied here instead.
     */
    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        if (node.equals(DEEP_FREEZE)) {
            this.freezeDurationTicks = Math.round(this.freezeDurationTicks * DEEP_FREEZE_DURATION_MULTIPLIER);
        }
    }

    private EnemyMob findTarget() {
        List<EnemyMob> inRange = InRangeTargetQuery.ofType(this.centerX, this.centerY, this.rangeReal(), EnemyMob.Type.NORMAL)
                .matching(this.context.enemies());
        return new FurthestAlongPathSelector().selectFrom(inRange).orElse(null);
    }

    public void doTick(int gameTime) {
        if (this.coolDown > 0) {
            this.coolDown--;
        } else {
            this.currentTarget = this.findTarget();
            if (this.currentTarget != null) {
                this.fireAt(this.currentTarget);
                this.coolDown = this.coolDownCurrent();
            }
        }
        // No target: hold the last heading rather than snapping back to a neutral angle - see
        // TurretAim's class doc on skipping tick() while idle.
        if (this.currentTarget != null) {
            this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, this.currentTarget.getX(), this.currentTarget.getY()));
        }
    }

    private void fireAt(EnemyMob target) {
        this.context.projectiles().add(new MissileProjectile(this.centerX, this.centerY, target,
                this.context.enemies(), PROJECTILE_SPEED, this::onImpact));
    }

    private void onImpact(EnemyMob target) {
        this.dealDamage(target, Damage.magic(this.damageCurrent()));
        target.applyEffect(Effect.freeze(this.freezeDurationTicks, d -> this.dealDamage(target, d)));
    }

    public EnemyMob getCurrentTarget() {
        return this.currentTarget;
    }

    int getFreezeDurationTicks() {
        return this.freezeDurationTicks;
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSeekerTower(this);
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
