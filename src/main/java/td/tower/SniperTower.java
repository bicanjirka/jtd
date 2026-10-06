package td.tower;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.targeting.FurthestAlongPathSelector;
import td.tower.targeting.HighestHealthSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.TargetSelector;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.tower.upgrade.UpgradeTier;
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
    public static final float DAMAGE_POINTS = 30f;
    public static final float RANGE = 3.8f;
    public static final float CRIT_CHANCE = 0.15f;
    private static final double MAX_TURN_RADIANS_PER_TICK = 0.4;

    private static final UpgradeNode FOCUSED_OPTICS_1 = UpgradeTier.HEAD_1.node("sniper.head.focused_optics.1",
            "Focused Optics", PRICE)
            .withBuff(TowerBuff.damage(0.2f));
    private static final UpgradeNode FOCUSED_OPTICS_2 = UpgradeTier.HEAD_2.node("sniper.head.focused_optics.2",
            "Focused Optics II", PRICE)
            .withBuff(TowerBuff.damage(0.2f).withFireRate(0.25f))
            .after(FOCUSED_OPTICS_1);
    private static final UpgradeNode MARKSMANS_EYE_1 = UpgradeTier.HEAD_1.node("sniper.head.marksmans_eye.1",
            "Marksman's Eye", PRICE)
            .withBuff(TowerBuff.critChance(0.15f));
    private static final UpgradeNode MARKSMANS_EYE_2 = UpgradeTier.HEAD_2.node("sniper.head.marksmans_eye.2",
            "Marksman's Eye II", PRICE)
            .withBuff(TowerBuff.critChance(0.2f).withArmorPenetration(0.5f))
            .after(MARKSMANS_EYE_1);
    private static final UpgradeNode MARKED_ROUND = UpgradeTier.SPECIAL.node("sniper.special.marked_round",
            "Marked Round", PRICE)
            .withExtraEffect("crits apply Vulnerable, +15% damage taken, stacks x3");
    private static final UpgradeNode FIFTH_SHOT = UpgradeTier.SPECIAL.node("sniper.special.fifth_shot",
            "Fifth Shot", PRICE)
            .withExtraEffect("every 5th shot is a guaranteed crit, and its crits deal 250%");
    private static final UpgradeNode MOMENTUM = UpgradeTier.SPECIAL.node("sniper.special.momentum", "Momentum", PRICE)
            .withExtraEffect("post-crit shot deals 500% and ignores armor and plating; a kill grants +100% fire "
                    + "rate for 5s");
    private static final int FIFTH_SHOT_INTERVAL = 5;
    private static final float FIFTH_SHOT_CRIT_MULTIPLIER = 2.5f;
    private static final int MOMENTUM_DAMAGE_MULTIPLIER = 5;
    /** Cooldown cut for {@link #MOMENTUM_KILL_BUFF_SECONDS} after a kill. */
    private static final float MOMENTUM_KILL_FIRE_RATE = 0.5f;
    private static final float MOMENTUM_KILL_BUFF_SECONDS = 5f;

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE))
            .with(FOCUSED_OPTICS_1, FOCUSED_OPTICS_2, MARKSMANS_EYE_1, MARKSMANS_EYE_2, MARKED_ROUND, FIFTH_SHOT, MOMENTUM)
            .withChoice(ExclusiveChoice.oneOf(FOCUSED_OPTICS_1, MARKSMANS_EYE_1))
            .withChoice(ExclusiveChoice.specials(MARKED_ROUND, FIFTH_SHOT, MOMENTUM));

    private static final int COOLDOWN_MAX = 39;
    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private volatile TargetSelector targetSelector = new FurthestAlongPathSelector();
    private int coolDown = 0;
    private EnemyMob currentTarget;
    private boolean lastShotCritical;
    private int shotsFired;
    private boolean momentumCharged;

    public SniperTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SNIPER, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX).withCritChance(CRIT_CHANCE), context, x, y);
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
                attack = attack.withGuaranteedCrit();
            }
        }
        if (this.momentumCharged) {
            damage *= MOMENTUM_DAMAGE_MULTIPLIER;
            attack = attack.withArmorPenetration(1f, 0f).withPlatingPenetration(1f);
        }
        boolean critical = this.dealDamage(target, Damage.physical(damage), attack);
        this.momentumCharged = critical && this.upgrades().owns(MOMENTUM.id());
        if (critical && this.upgrades().owns(MARKED_ROUND.id())) {
            this.applyVulnerable(target, 1);
        }
        return critical;
    }

    /** Momentum: a kill buffs the fire rate for a few seconds; another kill restarts the clock. */
    @Override
    protected void onKill(EnemyMob killed) {
        if (this.upgrades().owns(MOMENTUM.id())) {
            this.grantTimedBuff(TowerBuff.fireRate(MOMENTUM_KILL_FIRE_RATE),
                    Math.round(MOMENTUM_KILL_BUFF_SECONDS * TICKS_PER_SECOND));
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

    @Override
    protected List<BehaviourLine> behaviours() {
        boolean special = this.upgrades().countIn(UpgradeSlot.SPECIAL) > 0;
        BehaviourLine targets = new BehaviourLine(BehaviourMarker.TARGETING, "Targets", special ? "most health" : "first");
        if (!this.upgrades().owns(MARKED_ROUND.id())) {
            return List.of(targets);
        }
        return List.of(targets, new BehaviourLine(BehaviourMarker.VULNERABLE, "Crits apply", "vulnerable"));
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSniperTower(this);
    }
}
