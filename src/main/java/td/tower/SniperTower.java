package td.tower;

import td.damage.AttackProfile;
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
 * The cheap single-target tower. Fires at the visible enemy in range furthest along the path; any
 * {@code SPECIAL} upgrade switches it to the enemy with the most health. The turret turns at a
 * capped rate and holds its heading when idle.
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

    private static final UpgradeNode FOCUSED_OPTICS_1 = UpgradeNode.of("sniper.head.focused_optics.1",
            UpgradeSlot.HEAD, "Focused Optics", 25)
            .withBuff(TowerBuff.damage(0.2f))
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD));
    private static final UpgradeNode FOCUSED_OPTICS_2 = UpgradeNode.of("sniper.head.focused_optics.2",
            UpgradeSlot.HEAD, "Focused Optics II", 38)
            .withBuff(TowerBuff.damage(0.2f).withFireRate(0.25f))
            .withRequires(UpgradeCondition.owns(FOCUSED_OPTICS_1.id()));
    private static final UpgradeNode MARKSMANS_EYE_1 = UpgradeNode.of("sniper.head.marksmans_eye.1",
            UpgradeSlot.HEAD, "Marksman's Eye", 30)
            .withBuff(TowerBuff.critChance(0.15f))
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD))
            .withGate(new KillCountCondition(15));
    private static final UpgradeNode MARKSMANS_EYE_2 = UpgradeNode.of("sniper.head.marksmans_eye.2",
            UpgradeSlot.HEAD, "Marksman's Eye II", 45)
            .withBuff(TowerBuff.critChance(0.2f).withArmorPenetration(0.5f))
            .withRequires(UpgradeCondition.owns(MARKSMANS_EYE_1.id()))
            .withGate(new DamageDealtCondition(20000));
    /** Not implemented yet (TODO.md). */
    private static final UpgradeNode MARKED_ROUND = UpgradeNode.of("sniper.special.marked_round", UpgradeSlot.SPECIAL,
            "Marked Round", 20)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new KillCountCondition(10))
            .withExtraEffect("crits apply Vulnerable, +15% damage taken, stacks x3");
    private static final UpgradeNode FIFTH_SHOT = UpgradeNode.of("sniper.special.fifth_shot", UpgradeSlot.SPECIAL,
            "Fifth Shot", 20)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new KillCountCondition(15))
            .withExtraEffect("every 5th shot is a guaranteed crit, and its crits deal 250%");
    /** Its fire-rate burst on a kill is not implemented yet (TODO.md). */
    private static final UpgradeNode MOMENTUM = UpgradeNode.of("sniper.special.momentum", UpgradeSlot.SPECIAL,
            "Momentum", 20)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new KillCountCondition(20))
            .withExtraEffect("post-crit shot deals 500% and ignores armor and plating; a kill grants +100% fire "
                    + "rate for 5s");
    private static final int FIFTH_SHOT_INTERVAL = 5;
    private static final float FIFTH_SHOT_CRIT_MULTIPLIER = 2.5f;
    private static final int MOMENTUM_DAMAGE_MULTIPLIER = 5;

    private static final UpgradeTree TREE = UpgradeTree.of(BASE_RANGE, AWAKEN, FOCUSED_OPTICS_1, FOCUSED_OPTICS_2,
            MARKSMANS_EYE_1, MARKSMANS_EYE_2, MARKED_ROUND, FIFTH_SHOT, MOMENTUM);

    private static final int COOLDOWN_MAX = 39;
    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private volatile TargetSelector targetSelector = new FurthestAlongPathSelector();
    private int coolDown = 0;
    private EnemyMob currentTarget;
    private boolean lastShotCritical;
    private int shotsFired;
    private boolean momentumCharged;

    public SniperTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SNIPER, PRICE,
                new TowerBaseStats(DAMAGE, RANGE, COOLDOWN_MAX).withCritChance(CRIT_CHANCE), context, x, y);
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    /**
     * A {@code SPECIAL} tower's pricier shots go to the enemy with the most health left, not to
     * finishing off the nearly dead.
     */
    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        if (node.slot() == UpgradeSlot.SPECIAL) {
            this.targetSelector = new HighestHealthSelector();
        }
    }

    private EnemyMob findEnemy() {
        List<EnemyMob> inRange = InRangeTargetQuery.visible(this.centerX, this.centerY, this.rangeReal())
                .matching(this.context.enemies());
        return this.targetSelector.selectFrom(inRange).orElse(null);
    }

    public void doTick(int gameTime) {
        if (this.coolDown > 0) {
            this.coolDown--;
        } else {
            this.currentTarget = this.findEnemy();
            if (this.currentTarget != null) {
                this.lastShotCritical = this.fire(this.currentTarget);
                this.coolDown = this.coolDownCurrent();
            }
        }
        if (this.currentTarget != null) {
            this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, this.currentTarget.getX(), this.currentTarget.getY()));
        }
    }

    /**
     * One shot, shaped by whichever {@code SPECIAL} node is owned: Fifth Shot forces every fifth
     * shot to crit and raises the crit multiplier; Momentum turns the shot after a crit into a
     * five-fold one that ignores armor and plating.
     */
    private boolean fire(EnemyMob target) {
        this.shotsFired++;
        int damage = this.damageCurrent();
        AttackProfile attack = this.stats().attack();
        if (this.upgrades().owns(FIFTH_SHOT.id())) {
            attack = attack.withCritMultiplier(FIFTH_SHOT_CRIT_MULTIPLIER);
            if (this.shotsFired % FIFTH_SHOT_INTERVAL == 0) {
                attack = attack.withCritChance(1f);
            }
        }
        if (this.momentumCharged) {
            damage *= MOMENTUM_DAMAGE_MULTIPLIER;
            attack = attack.withArmorPenetration(1f, 0f).withPlatingPenetration(1f);
        }
        boolean critical = this.dealDamage(target, Damage.physical(damage), attack);
        this.momentumCharged = critical && this.upgrades().owns(MOMENTUM.id());
        return critical;
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

    @Override
    protected List<BehaviourLine> behaviours() {
        boolean special = this.upgrades().tip(UpgradeSlot.SPECIAL).isPresent();
        return List.of(new BehaviourLine(BehaviourMarker.TARGETING, "Targets", special ? "most health" : "first"));
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSniperTower(this);
    }
}
