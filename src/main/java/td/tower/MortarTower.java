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

/**
 * Lobs an unguided shell at the visible enemy furthest along the path. The shell flies to where the
 * enemy was when fired, so fast enemies can dodge it; the blast deals physical damage with distance
 * falloff and chills everything it reaches.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class MortarTower extends AbstractTower {

    public static final int PRICE = 30;
    public static final float DAMAGE_POINTS = 20f;
    public static final float RANGE = 4.0f;
    public static final float SPLASH_RADIUS_BASE = 2.0f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.3;
    private static final float PROJECTILE_SPEED = 40f;
    private static final float CHILL_AMOUNT_BASE = 0.5f;
    private static final int CHILL_DURATION_TICKS_BASE = 40;
    /** Splash radius multiplier from the second damage upgrade. */
    private static final float SIEGE_ROUNDS_SPLASH_MULTIPLIER = 1.4f;
    /** Fragmentation Rounds: shrapnel reaches this far past the main splash, as a multiple of its radius. */
    private static final float SHRAPNEL_RING_MULTIPLIER = 1.75f;
    /** Share of weapon damage each piece of shrapnel deals, with no falloff. */
    private static final float SHRAPNEL_DAMAGE_SHARE = 0.25f;
    private static final float SHRAPNEL_CHILL_DURATION_SHARE = 0.5f;

    private static final UpgradeNode SIEGE_ROUNDS_1 = UpgradeTier.HEAD_1.node("mortar.head.siege_rounds.1",
            "Siege Rounds", PRICE)
            .withBuff(TowerBuff.damage(0.3f))
            .withGate(new DamageDealtCondition(150));
    private static final UpgradeNode SIEGE_ROUNDS_2 = UpgradeTier.HEAD_2.node("mortar.head.siege_rounds.2",
            "Siege Rounds II", PRICE)
            .withBuff(TowerBuff.damage(0.25f))
            .after(SIEGE_ROUNDS_1)
            .withGate(new DamageDealtCondition(300))
            .withExtraEffect("+40% splash radius");
    private static final UpgradeNode FRAGMENTATION_ROUNDS_1 = UpgradeTier.HEAD_1.node("mortar.head.fragmentation_rounds.1",
            "Fragmentation Rounds", PRICE)
            .withGate(new KillCountCondition(12))
            .withExtraEffect("shrapnel deals 25% weapon damage in a wider ring past the main splash");
    private static final UpgradeNode FRAGMENTATION_ROUNDS_2 = UpgradeTier.HEAD_2.node("mortar.head.fragmentation_rounds.2",
            "Fragmentation Rounds II", PRICE)
            .after(FRAGMENTATION_ROUNDS_1)
            .withGate(new ClusterCondition(2))
            .withExtraEffect("shrapnel also applies this tower's chill, at half duration");
    private static final UpgradeNode CURSED_SHRAPNEL = UpgradeTier.SPECIAL.node("mortar.special.cursed_shrapnel",
            "Cursed Shrapnel", PRICE)
            .withGate(new KillCountCondition(20))
            .withExtraEffect("every enemy caught in the blast gets a guaranteed Vulnerable stack (cap 3), refreshed on every hit");

    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(PRICE))
            .with(SIEGE_ROUNDS_1, SIEGE_ROUNDS_2, FRAGMENTATION_ROUNDS_1, FRAGMENTATION_ROUNDS_2, CURSED_SHRAPNEL)
            .withChoice(ExclusiveChoice.oneOf(SIEGE_ROUNDS_1, FRAGMENTATION_ROUNDS_1));

    private static final int COOLDOWN_MAX = 50;
    private final float chillAmount = CHILL_AMOUNT_BASE;
    private final int chillDurationTicks = CHILL_DURATION_TICKS_BASE;
    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private volatile float splashRadius;
    private int coolDown = 0;
    private EnemyMob currentTarget;

    public MortarTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.MORTAR, new TowerBaseStats(DAMAGE_POINTS, RANGE, COOLDOWN_MAX), context, x, y);
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

    private void fireAt(EnemyMob target) {
        this.context.projectiles().add(new CannonballProjectile(this.centerX, this.centerY, target.getX(), target.getY(),
                PROJECTILE_SPEED, this::onImpact));
    }

    private void onImpact(double x, double y) {
        List<EnemyMob> hit = InRangeTargetQuery.everyone((int) Math.round(x), (int) Math.round(y), this.splashRadius)
                .matching(this.context.enemies());
        for (EnemyMob enemy : hit) {
            double dx = x - enemy.getX();
            double dy = y - enemy.getY();
            float r2 = (float) (dx * dx + dy * dy);
            int amount = Math.round(this.damageCurrent() * (1 - r2 / (this.splashRadius * this.splashRadius)));
            this.dealDamage(enemy, Damage.physical(amount));
            enemy.applyEffect(Effect.chill(this.chillAmount, this.chillDurationTicks, d -> this.dealDamage(enemy, d)));
            if (this.upgrades().owns(CURSED_SHRAPNEL.id())) {
                this.applyVulnerable(enemy, 1);
            }
        }
        if (this.upgrades().owns(FRAGMENTATION_ROUNDS_1.id())) {
            this.shrapnel((int) Math.round(x), (int) Math.round(y), hit);
        }
    }

    /**
     * Fragmentation Rounds: everything past the main splash but within the wider ring takes a fixed
     * share of weapon damage, and with the second node the tower's chill for half as long.
     */
    private void shrapnel(int x, int y, List<EnemyMob> alreadyHit) {
        Damage shard = Damage.physical(Math.round(this.damageCurrent() * SHRAPNEL_DAMAGE_SHARE));
        int chillTicks = Math.round(this.chillDurationTicks * SHRAPNEL_CHILL_DURATION_SHARE);
        for (EnemyMob enemy : InRangeTargetQuery.everyone(x, y, this.splashRadius * SHRAPNEL_RING_MULTIPLIER)
                .matching(this.context.enemies())) {
            if (alreadyHit.contains(enemy)) {
                continue;
            }
            this.dealDamage(enemy, shard);
            if (this.upgrades().owns(FRAGMENTATION_ROUNDS_2.id())) {
                enemy.applyEffect(Effect.chill(this.chillAmount, chillTicks, d -> this.dealDamage(enemy, d)));
            }
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
        List<BehaviourLine> lines = new ArrayList<>();
        lines.add(new BehaviourLine(BehaviourMarker.CHILL, "Chills",
                BehaviourLine.percent(this.chillAmount) + ", " + BehaviourLine.seconds(this.chillDurationTicks)));
        lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Targets", "first"));
        if (this.upgrades().owns(FRAGMENTATION_ROUNDS_1.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.TARGETING, "Shrapnel", BehaviourLine.percent(SHRAPNEL_DAMAGE_SHARE) + " damage"));
        }
        if (this.upgrades().owns(CURSED_SHRAPNEL.id())) {
            lines.add(new BehaviourLine(BehaviourMarker.VULNERABLE, "Blast applies", "vulnerable"));
        }
        return lines;
    }

    @Override
    protected String description() {
        return "Lobs a slow, unguided shell.";
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitMortarTower(this);
    }
}
