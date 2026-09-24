package td.tower;

import td.damage.Damage;
import td.effect.Effect;
import td.enemy.EnemyMob;
import td.projectile.MissileProjectile;
import td.tower.buff.TowerBuff;
import td.tower.targeting.FurthestAlongPathSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.upgrade.DamageDealtCondition;
import td.tower.upgrade.KillCountCondition;
import td.tower.upgrade.StandardBaseSlot;
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
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class SeekerTower extends AbstractTower {

    public static final int PRICE = 35;
    public static final int DAMAGE = 2600;
    public static final float RANGE = 4.5f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.3;
    private static final float PROJECTILE_SPEED = 35f;
    private static final int FREEZE_DURATION_TICKS_BASE = 30;
    private static final float DEEP_FREEZE_DURATION_MULTIPLIER = 1.75f;

    private static final UpgradeNode BASE_RANGE = StandardBaseSlot.rangeNode(21);
    private static final UpgradeNode AWAKEN = StandardBaseSlot.awakenNode(35);

    /**
     * Faster reloading, earned by this tower's own proven kill record.
     */
    private static final UpgradeNode TWIN_WARHEAD_1 = UpgradeNode.of("seeker.head.twin_warhead.1", UpgradeSlot.HEAD,
            "Twin Warhead", 30)
            .withBuff(TowerBuff.fireRate(0.3f))
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD))
            .withGate(new KillCountCondition(10));
    /**
     * Faster reloading still, and fires two independently-retargeting missiles instead of one.
     */
    private static final UpgradeNode TWIN_WARHEAD_2 = UpgradeNode.of("seeker.head.twin_warhead.2", UpgradeSlot.HEAD,
            "Twin Warhead II", 45)
            .withBuff(TowerBuff.fireRate(0.25f))
            .withRequires(UpgradeCondition.owns(TWIN_WARHEAD_1.id()))
            .withGate(new DamageDealtCondition(25000))
            .withExtraEffect("fires two independently-retargeting missiles instead of one");
    /**
     * More damage, earned by this tower's own proven kill record.
     */
    private static final UpgradeNode DEEP_FREEZE_1 = UpgradeNode.of("seeker.head.deep_freeze.1", UpgradeSlot.HEAD,
            "Deep Freeze", 35)
            .withBuff(TowerBuff.damage(0.3f))
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD))
            .withGate(new KillCountCondition(12));
    /**
     * More damage still and a longer freeze; killing a frozen enemy shatters it for splash
     * damage once the on-kill-secondary-trigger primitive exists - see TODO.md.
     */
    private static final UpgradeNode DEEP_FREEZE_2 = UpgradeNode.of("seeker.head.deep_freeze.2", UpgradeSlot.HEAD,
            "Deep Freeze II", 53)
            .withBuff(TowerBuff.damage(0.25f))
            .withRequires(UpgradeCondition.owns(DEEP_FREEZE_1.id()))
            .withGate(new KillCountCondition(25))
            .withExtraEffect("+75% freeze duration, killing a frozen enemy shatters it for 50% weapon damage splash");
    /**
     * Impact applies a Vulnerable stack (2 if the target was already frozen or slowed), once
     * that primitive exists - see TODO.md.
     */
    private static final UpgradeNode HOMING_CURSE = UpgradeNode.of("seeker.special.homing_curse", UpgradeSlot.SPECIAL,
            "Homing Curse", 70)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new KillCountCondition(20))
            .withExtraEffect("impact applies 1 Vulnerable stack, or 2 if the target was already frozen or slowed");

    private static final UpgradeTree TREE = UpgradeTree.of(BASE_RANGE, AWAKEN, TWIN_WARHEAD_1, TWIN_WARHEAD_2,
            DEEP_FREEZE_1, DEEP_FREEZE_2, HOMING_CURSE);

    /**
     * Ticks between shots before any fire-rate buff - paired with this tower's damage.
     */
    private static final int COOLDOWN_MAX = 45;

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private volatile int freezeDurationTicks = FREEZE_DURATION_TICKS_BASE;
    private volatile boolean twinMissiles = false;
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
     * Bonuses that aren't a {@link TowerBuff} axis are applied here instead.
     */
    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        if (node.equals(DEEP_FREEZE_2)) {
            this.freezeDurationTicks = Math.round(this.freezeDurationTicks * DEEP_FREEZE_DURATION_MULTIPLIER);
        } else if (node.equals(TWIN_WARHEAD_2)) {
            this.twinMissiles = true;
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
        if (this.currentTarget != null) {
            this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, this.currentTarget.getX(), this.currentTarget.getY()));
        }
    }

    /**
     * Fires one missile, or two independently-retargeting ones once Twin Warhead II is owned -
     * {@code MissileProjectile} already retargets independently per instance, so firing two is
     * reusing the existing projectile class twice, not a new primitive.
     */
    private void fireAt(EnemyMob target) {
        this.context.projectiles().add(new MissileProjectile(this.centerX, this.centerY, target,
                this.context.enemies(), PROJECTILE_SPEED, this::onImpact));
        if (this.twinMissiles) {
            this.context.projectiles().add(new MissileProjectile(this.centerX, this.centerY, target,
                    this.context.enemies(), PROJECTILE_SPEED, this::onImpact));
        }
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
