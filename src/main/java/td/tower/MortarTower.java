package td.tower;

import td.damage.Damage;
import td.effect.Effect;
import td.enemy.EnemyMob;
import td.projectile.CannonballProjectile;
import td.tower.buff.TowerBuff;
import td.tower.targeting.FurthestAlongPathSelector;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.upgrade.ClusterCondition;
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
 * Lobs an unguided shell at the visible enemy furthest along the path. The shell flies to where the
 * enemy was when fired, so fast enemies can dodge it; the blast deals physical damage with distance
 * falloff and slows everything it reaches.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class MortarTower extends AbstractTower {

    public static final int PRICE = 30;
    public static final int DAMAGE = 2000;
    public static final float RANGE = 4.0f;
    public static final float SPLASH_RADIUS_BASE = 2.0f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.3;
    private static final float PROJECTILE_SPEED = 40f;
    private static final float SLOW_MULTIPLIER_BASE = 0.5f;
    private static final int SLOW_DURATION_TICKS_BASE = 40;
    /** Splash radius multiplier from the second damage upgrade. */
    private static final float SIEGE_ROUNDS_SPLASH_MULTIPLIER = 1.4f;

    private static final UpgradeNode BASE_RANGE = StandardBaseSlot.rangeNode(18);
    private static final UpgradeNode AWAKEN = StandardBaseSlot.awakenNode(30);

    private static final UpgradeNode SIEGE_ROUNDS_1 = UpgradeNode.of("mortar.head.siege_rounds.1", UpgradeSlot.HEAD,
            "Siege Rounds", 35)
            .withBuff(TowerBuff.damage(0.3f))
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD))
            .withGate(new DamageDealtCondition(15000));
    private static final UpgradeNode SIEGE_ROUNDS_2 = UpgradeNode.of("mortar.head.siege_rounds.2", UpgradeSlot.HEAD,
            "Siege Rounds II", 53)
            .withBuff(TowerBuff.damage(0.25f))
            .withRequires(UpgradeCondition.owns(SIEGE_ROUNDS_1.id()))
            .withGate(new DamageDealtCondition(30000))
            .withExtraEffect("+40% splash radius");
    /** Not implemented yet (TODO.md). */
    private static final UpgradeNode FRAGMENTATION_ROUNDS_1 = UpgradeNode.of("mortar.head.fragmentation_rounds.1",
            UpgradeSlot.HEAD, "Fragmentation Rounds", 30)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD))
            .withGate(new KillCountCondition(12))
            .withExtraEffect("shrapnel deals 25% weapon damage in a wider ring past the main splash");
    /** Not implemented yet (TODO.md). */
    private static final UpgradeNode FRAGMENTATION_ROUNDS_2 = UpgradeNode.of("mortar.head.fragmentation_rounds.2",
            UpgradeSlot.HEAD, "Fragmentation Rounds II", 45)
            .withRequires(UpgradeCondition.owns(FRAGMENTATION_ROUNDS_1.id()))
            .withGate(new ClusterCondition(2))
            .withExtraEffect("shrapnel also applies this tower's slow, at half duration");
    /** Not implemented yet (TODO.md). */
    private static final UpgradeNode CURSED_SHRAPNEL = UpgradeNode.of("mortar.special.cursed_shrapnel",
            UpgradeSlot.SPECIAL, "Cursed Shrapnel", 60)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new KillCountCondition(20))
            .withExtraEffect("every enemy caught in the blast gets a guaranteed Vulnerable stack (cap 3), refreshed on every hit");

    private static final UpgradeTree TREE = UpgradeTree.of(BASE_RANGE, AWAKEN, SIEGE_ROUNDS_1, SIEGE_ROUNDS_2,
            FRAGMENTATION_ROUNDS_1, FRAGMENTATION_ROUNDS_2, CURSED_SHRAPNEL);

    private static final int COOLDOWN_MAX = 50;
    private final float slowMultiplier = SLOW_MULTIPLIER_BASE;
    private final int slowDurationTicks = SLOW_DURATION_TICKS_BASE;
    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private volatile float splashRadius;
    private int coolDown = 0;
    private EnemyMob currentTarget;

    public MortarTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.MORTAR, PRICE, new TowerBaseStats(DAMAGE, RANGE, COOLDOWN_MAX), context, x, y);
        this.splashRadius = SPLASH_RADIUS_BASE * context.getBoard().scale();
    }

    @Override
    public UpgradeTree upgradeTree() {
        return TREE;
    }

    @Override
    protected void onUpgradeBought(UpgradeNode node) {
        if (node.equals(SIEGE_ROUNDS_2)) {
            this.splashRadius *= SIEGE_ROUNDS_SPLASH_MULTIPLIER;
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

    private void fireAt(EnemyMob target) {
        this.context.projectiles().add(new CannonballProjectile(this.centerX, this.centerY, target.getX(), target.getY(),
                PROJECTILE_SPEED, this::onImpact));
    }

    private void onImpact(double x, double y) {
        List<EnemyMob> hit = InRangeTargetQuery.anyType((int) Math.round(x), (int) Math.round(y), this.splashRadius)
                .matching(this.context.enemies());
        for (EnemyMob enemy : hit) {
            double dx = x - enemy.getX();
            double dy = y - enemy.getY();
            float r2 = (float) (dx * dx + dy * dy);
            int amount = Math.round(this.damageCurrent() * (1 - r2 / (this.splashRadius * this.splashRadius)));
            this.dealDamage(enemy, Damage.physical(amount));
            enemy.applyEffect(Effect.slow(this.slowMultiplier, this.slowDurationTicks, d -> this.dealDamage(enemy, d)));
        }
    }

    public EnemyMob getCurrentTarget() {
        return this.currentTarget;
    }

    float getSplashRadius() {
        return this.splashRadius;
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    @Override
    protected List<TowerStatLine> ownStats() {
        return List.of(new TowerStatLine(TowerStat.SPLASH_RADIUS, SPLASH_RADIUS_BASE,
                this.splashRadius / this.context.getBoard().scale()));
    }

    @Override
    protected List<BehaviourLine> behaviours() {
        return List.of(new BehaviourLine(BehaviourMarker.SLOW, "Slows",
                        BehaviourLine.percent(1f - this.slowMultiplier) + ", " + BehaviourLine.seconds(this.slowDurationTicks)),
                new BehaviourLine(BehaviourMarker.TARGETING, "Targets", "first"));
    }

    @Override
    protected String description() {
        return "Lobs a slow, unguided shell.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitMortarTower(this);
    }
}
