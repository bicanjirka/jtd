package td.tower;

import td.damage.Damage;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.targeting.FurthestAlongPathSelector;
import td.tower.targeting.HighestHealthSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.TargetSelector;
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
 * "Sniper tower" - the cheap single-target one. Fires at whichever visible enemy in range
 * is furthest along the path, which is the usual right answer since that enemy is closest to
 * costing a life - until it specializes into {@code SPECIAL}, at which point it switches to the
 * highest-current-health enemy instead (see {@link #onUpgradeBought}). Its turret head sweeps
 * toward the target at a capped rate rather than snapping, and holds its last heading when it
 * has no target (see {@link TurretAim}).
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class SniperTower extends AbstractTower {

    public static final int PRICE = 10;
    public static final int DAMAGE = 3000;
    public static final float RANGE = 3.8f;
    public static final float CRIT_CHANCE = 0.15f;
    private static final double MAX_TURN_RADIANS_PER_TICK = 0.4;

    private static final UpgradeNode BASE_RANGE = StandardBaseSlot.rangeNode(6);
    private static final UpgradeNode AWAKEN = StandardBaseSlot.awakenNode(10);

    /**
     * More damage, earned by proven output alone - no track record needed.
     */
    private static final UpgradeNode FOCUSED_OPTICS_1 = UpgradeNode.of("sniper.head.focused_optics.1",
            UpgradeSlot.HEAD, "Focused Optics", 25)
            .withBuff(TowerBuff.damage(0.2f))
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD));
    /**
     * More damage still, plus a faster reload.
     */
    private static final UpgradeNode FOCUSED_OPTICS_2 = UpgradeNode.of("sniper.head.focused_optics.2",
            UpgradeSlot.HEAD, "Focused Optics II", 38)
            .withBuff(TowerBuff.damage(0.2f).withFireRate(0.25f))
            .withRequires(UpgradeCondition.owns(FOCUSED_OPTICS_1.id()));
    /**
     * More crit chance - a marksman's proven aim starts placing shots that count extra more often.
     */
    private static final UpgradeNode MARKSMANS_EYE_1 = UpgradeNode.of("sniper.head.marksmans_eye.1",
            UpgradeSlot.HEAD, "Marksman's Eye", 30)
            .withBuff(TowerBuff.critChance(0.15f))
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD))
            .withGate(new KillCountCondition(15));
    /**
     * More crit chance still, and (once the armor/shield-bypass primitive exists) partial
     * armor/shield penetration - see TODO.md.
     */
    private static final UpgradeNode MARKSMANS_EYE_2 = UpgradeNode.of("sniper.head.marksmans_eye.2",
            UpgradeSlot.HEAD, "Marksman's Eye II", 45)
            .withBuff(TowerBuff.critChance(0.2f))
            .withRequires(UpgradeCondition.owns(MARKSMANS_EYE_1.id()))
            .withGate(new DamageDealtCondition(20000))
            .withExtraEffect("ignores 50% of armor and shields");
    /**
     * Crits apply Vulnerable (stacks x3) once that primitive exists - see TODO.md.
     */
    private static final UpgradeNode MARKED_ROUND = UpgradeNode.of("sniper.special.marked_round", UpgradeSlot.SPECIAL,
            "Marked Round", 20)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new KillCountCondition(10))
            .withExtraEffect("crits apply Vulnerable, +15% damage taken, stacks x3");
    /**
     * Every 5th shot is a guaranteed critical hit dealing 250% damage, once the guaranteed-crit
     * primitive exists - see TODO.md.
     */
    private static final UpgradeNode FIFTH_SHOT = UpgradeNode.of("sniper.special.fifth_shot", UpgradeSlot.SPECIAL,
            "Fifth Shot", 20)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new KillCountCondition(15))
            .withExtraEffect("every 5th shot is a guaranteed crit dealing 250%");
    /**
     * A crit's next shot deals 500% damage and fully bypasses armor/shields; a kill grants
     * +100% fire rate for 5s - both once their primitives exist, see TODO.md.
     */
    private static final UpgradeNode MOMENTUM = UpgradeNode.of("sniper.special.momentum", UpgradeSlot.SPECIAL,
            "Momentum", 20)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new KillCountCondition(20))
            .withExtraEffect("post-crit shot deals 500% and ignores armor; a kill grants +100% fire rate for 5s");

    private static final UpgradeTree TREE = UpgradeTree.of(BASE_RANGE, AWAKEN, FOCUSED_OPTICS_1, FOCUSED_OPTICS_2,
            MARKSMANS_EYE_1, MARKSMANS_EYE_2, MARKED_ROUND, FIFTH_SHOT, MOMENTUM);

    /**
     * Ticks between shots before any fire-rate buff.
     */
    private static final int COOLDOWN_MAX = 39;
    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private volatile TargetSelector targetSelector = new FurthestAlongPathSelector();
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

    /**
     * Any {@code SPECIAL} node switches this tower's targeting from "furthest along the path"
     * to "highest current health" - deliberate, so a tower that's earned this slot stops
     * spending its (now pricier, gated) shots last-hitting nearly-dead enemies and instead
     * puts damage where an enemy still has the most health left to lose.
     */
    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        if (node.slot() == UpgradeSlot.SPECIAL) {
            this.targetSelector = new HighestHealthSelector();
        }
    }

    private EnemyMob findEnemy() {
        List<EnemyMob> inRange = InRangeTargetQuery.ofType(this.centerX, this.centerY, this.rangeReal(), EnemyMob.Type.NORMAL)
                .matching(this.context.enemies());
        return this.targetSelector.selectFrom(inRange).orElse(null);
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
