package td.tower;

import td.damage.Damage;
import td.damage.DamageType;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.projectile.MissileProjectile;
import td.tower.buff.TowerBuff;
import td.tower.targeting.FurthestAlongPathSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.upgrade.DamageDealtCondition;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.KillCountCondition;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Fires a homing missile at the visible enemy furthest along the path. On impact it deals magic
 * damage and freezes whichever enemy it reached, which may not be the one it was fired at.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class SeekerTower extends AbstractTower {

    public static final int PRICE = 35;
    public static final float DAMAGE_POINTS = 26f;
    public static final float RANGE = 4.5f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.3;
    private static final float PROJECTILE_SPEED = 35f;
    private static final int FREEZE_DURATION_TICKS_BASE = 30;
    private static final float DEEP_FREEZE_DURATION_MULTIPLIER = 1.75f;
    /** Share of weapon damage a shattered enemy's neighbours take. */
    private static final float SHATTER_DAMAGE_SHARE = 0.5f;
    private static final float SHATTER_RADIUS_CELLS = 1.5f;

    private static final UpgradeNode TWIN_WARHEAD_1 = UpgradeTier.HEAD_1.node("seeker.head.twin_warhead.1",
            "Twin Warhead", PRICE)
            .withBuff(TowerBuff.fireRate(0.3f))
            .withGate(new KillCountCondition(10));
    private static final UpgradeNode TWIN_WARHEAD_2 = UpgradeTier.HEAD_2.node("seeker.head.twin_warhead.2",
            "Twin Warhead II", PRICE)
            .withBuff(TowerBuff.fireRate(0.25f))
            .after(TWIN_WARHEAD_1)
            .withGate(new DamageDealtCondition(250))
            .withExtraEffect("fires two independently-retargeting missiles instead of one");
    private static final UpgradeNode DEEP_FREEZE_1 = UpgradeTier.HEAD_1.node("seeker.head.deep_freeze.1",
            "Deep Freeze", PRICE)
            .withBuff(TowerBuff.damage(0.3f))
            .withGate(new KillCountCondition(12));
    private static final UpgradeNode DEEP_FREEZE_2 = UpgradeTier.HEAD_2.node("seeker.head.deep_freeze.2",
            "Deep Freeze II", PRICE)
            .withBuff(TowerBuff.damage(0.25f))
            .after(DEEP_FREEZE_1)
            .withGate(new KillCountCondition(25))
            .withExtraEffect("+75% freeze duration, killing a frozen enemy shatters it for 50% weapon damage splash");
    private static final UpgradeNode HOMING_CURSE = UpgradeTier.SPECIAL.node("seeker.special.homing_curse",
            "Homing Curse", PRICE)
            .withGate(new KillCountCondition(20))
            .withExtraEffect("impact applies 1 Vulnerable stack, or 2 if the target was already frozen or chilled");

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE))
            .with(TWIN_WARHEAD_1, TWIN_WARHEAD_2, DEEP_FREEZE_1, DEEP_FREEZE_2, HOMING_CURSE)
            .withChoice(ExclusiveChoice.oneOf(TWIN_WARHEAD_1, DEEP_FREEZE_1));

    private static final int COOLDOWN_MAX = 45;

    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private volatile int freezeDurationTicks = FREEZE_DURATION_TICKS_BASE;
    private volatile boolean twinMissiles = false;
    private int coolDown = 0;
    private EnemyMob currentTarget;

    public SeekerTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SEEKER, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX), context, x, y);
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        if (node.equals(DEEP_FREEZE_2)) {
            this.freezeDurationTicks = Math.round(this.freezeDurationTicks * DEEP_FREEZE_DURATION_MULTIPLIER);
        } else if (node.equals(TWIN_WARHEAD_2)) {
            this.twinMissiles = true;
        }
    }

    private EnemyMob findTarget() {
        List<EnemyMob> inRange = InRangeTargetQuery.visible(this.centerX, this.centerY, this.rangeReal())
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

    /** Fires one missile, or two once the upgrade is owned; each retargets on its own. */
    private void fireAt(EnemyMob target) {
        this.context.projectiles().add(new MissileProjectile(this.centerX, this.centerY, target,
                this.context.enemies(), PROJECTILE_SPEED, this::onImpact));
        if (this.twinMissiles) {
            this.context.projectiles().add(new MissileProjectile(this.centerX, this.centerY, target,
                    this.context.enemies(), PROJECTILE_SPEED, this::onImpact));
        }
    }

    private void onImpact(EnemyMob target) {
        Set<EffectKind> before = target.activeEffectKinds();
        boolean controlled = before.contains(EffectKind.FREEZE) || before.contains(EffectKind.CHILL);
        this.dealDamage(target, Damage.magic(this.damageCurrent()));
        target.applyEffect(Effect.freeze(this.freezeDurationTicks, d -> this.dealDamage(target, d)));
        if (this.upgrades().owns(HOMING_CURSE.id())) {
            this.applyVulnerable(target, controlled ? 2 : 1);
        }
    }

    /** Deep Freeze II: a frozen enemy this tower kills shatters, hurting whatever stands near it. */
    @Override
    protected void onKill(EnemyMob killed) {
        if (!this.upgrades().owns(DEEP_FREEZE_2.id()) || !killed.activeEffectKinds().contains(EffectKind.FREEZE)) {
            return;
        }
        float radius = SHATTER_RADIUS_CELLS * this.context.getBoard().scale();
        Damage shatter = Damage.magic(Math.round(this.damageCurrent() * SHATTER_DAMAGE_SHARE));
        for (EnemyMob nearby : InRangeTargetQuery.everyone((int) killed.getX(), (int) killed.getY(), radius)
                .matching(this.context.enemies())) {
            this.dealDamage(nearby, shatter);
        }
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

    @Override
    protected DamageType damageType() {
        return DamageType.MAGIC;
    }

    @Override
    protected List<BehaviourLine> behaviours() {
        List<BehaviourLine> lines = new ArrayList<>();
        lines.add(new BehaviourLine(BehaviourMarker.FREEZE, "Freezes", BehaviourLine.seconds(this.freezeDurationTicks)));
        lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Targets", "first"));
        if (this.upgrades().owns(HOMING_CURSE.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.VULNERABLE, "Impact applies", "1 vulnerable, 2 if chilled"));
        }
        return lines;
    }

    @Override
    protected String description() {
        return "Fires a homing missile.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSeekerTower(this);
    }
}
