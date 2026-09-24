package td.tower;

import td.damage.Damage;
import td.effect.Effect;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.RandomSelector;
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
import java.util.Optional;

/**
 * "Splash tower" - splash damage. Picks a random visible enemy in range, then damages
 * everything within {@code spreadRadius} of it, falling off with the square of the distance
 * from the blast centre. The splash deliberately uses an any-type query, so it is one of the
 * two towers that can hurt ghosts even though it cannot target them directly.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class SplashTower extends AbstractTower {

    public static final int PRICE = 15;
    public static final int DAMAGE = 1600;
    public static final float RANGE = 3.2f;
    public static final float SPREAD_RADIUS_BASE = 1.75f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.25;
    private static final float SLOW_MULTIPLIER = 0.5f;
    private static final int SLOW_DURATION_TICKS = 40;

    /**
     * How much bigger "Blast Engineering" gives this tower's blast radius.
     */
    private static final float BLAST_ENGINEERING_SPREAD_MULTIPLIER = 1.3f;
    /**
     * How much "Blast Engineering II" softens the falloff curve's own {@code r2} term - lower
     * flattens it (closer to uniform damage across the blast) without changing its shape.
     */
    private static final float FALLOFF_SOFTENING_FACTOR = 0.5f;

    private static final UpgradeNode BASE_RANGE = StandardBaseSlot.rangeNode(9);
    private static final UpgradeNode AWAKEN = StandardBaseSlot.awakenNode(15);

    /**
     * A bigger blast - earned by this tower having proven itself already.
     */
    private static final UpgradeNode BLAST_ENGINEERING_1 = UpgradeNode.of("splash.head.blast_engineering.1",
            UpgradeSlot.HEAD, "Blast Engineering", 35)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD))
            .withGate(new DamageDealtCondition(10000))
            .withExtraEffect("+30% splash radius");
    /**
     * More damage, and a flatter falloff curve so the blast's edge hits nearly as hard as its centre.
     */
    private static final UpgradeNode BLAST_ENGINEERING_2 = UpgradeNode.of("splash.head.blast_engineering.2",
            UpgradeSlot.HEAD, "Blast Engineering II", 53)
            .withBuff(TowerBuff.damage(0.25f))
            .withRequires(UpgradeCondition.owns(BLAST_ENGINEERING_1.id()))
            .withGate(new DamageDealtCondition(20000))
            .withExtraEffect("flattens the falloff curve");
    /**
     * Fires 3 projectiles instead of 1, once the multi-projectile primitive exists - see TODO.md.
     */
    private static final UpgradeNode BLAST_ENGINEERING_3 = UpgradeNode.of("splash.head.blast_engineering.3",
            UpgradeSlot.HEAD, "Blast Engineering III", 70)
            .withRequires(UpgradeCondition.owns(BLAST_ENGINEERING_2.id()))
            .withGate(new KillCountCondition(20))
            .withExtraEffect("fires 3 projectiles instead of 1");
    /**
     * A faster reload - rewards a deliberately grouped placement rather than a solo one.
     */
    private static final UpgradeNode RAPID_BATTERY_1 = UpgradeNode.of("splash.head.rapid_battery.1", UpgradeSlot.HEAD,
            "Rapid Battery", 30)
            .withBuff(TowerBuff.fireRate(0.25f))
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD))
            .withGate(new KillCountCondition(8));
    /**
     * More damage and some crit chance.
     */
    private static final UpgradeNode RAPID_BATTERY_2 = UpgradeNode.of("splash.head.rapid_battery.2", UpgradeSlot.HEAD,
            "Rapid Battery II", 45)
            .withBuff(TowerBuff.damage(0.25f).withCritChance(0.15f))
            .withRequires(UpgradeCondition.owns(RAPID_BATTERY_1.id()))
            .withGate(new KillCountCondition(18));
    /**
     * A crit splashes 50% bigger, once the crit-triggered-behavior primitive exists - see TODO.md.
     */
    private static final UpgradeNode RAPID_BATTERY_3 = UpgradeNode.of("splash.head.rapid_battery.3", UpgradeSlot.HEAD,
            "Rapid Battery III", 60)
            .withRequires(UpgradeCondition.owns(RAPID_BATTERY_2.id()))
            .withGate(new DamageDealtCondition(25000))
            .withExtraEffect("crits splash 50% bigger");
    /**
     * A toxic damage-over-time on the blast, once the toxic-DoT primitive exists - see TODO.md.
     */
    private static final UpgradeNode TOXIC_BLOOM = UpgradeNode.of("splash.special.toxic_bloom", UpgradeSlot.SPECIAL,
            "Toxic Bloom", 30)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new DamageDealtCondition(15000))
            .withExtraEffect("splash applies a toxic damage-over-time");
    /**
     * A heavier, slower blast that also slows everything it hits; a killed enemy exploding is a
     * separate, not-yet-existing on-kill-trigger primitive - see TODO.md.
     */
    private static final UpgradeNode CONCUSSIVE_BLAST = UpgradeNode.of("splash.special.concussive_blast",
            UpgradeSlot.SPECIAL, "Concussive Blast", 30)
            .withBuff(TowerBuff.fireRate(-0.5f))
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new KillCountCondition(15))
            .withExtraEffect("-50% fire rate, blast applies slow, killed enemies explode");
    /**
     * A crit fires at every enemy in range instead of just the one target, once the
     * crit-triggered-behavior-override primitive exists - see TODO.md.
     */
    private static final UpgradeNode OVERPRESSURE = UpgradeNode.of("splash.special.overpressure", UpgradeSlot.SPECIAL,
            "Overpressure", 30)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL))
            .withGate(new KillCountCondition(25))
            .withExtraEffect("on crit, the next shot fires at every enemy in range");

    private static final UpgradeTree TREE = UpgradeTree.of(BASE_RANGE, AWAKEN, BLAST_ENGINEERING_1,
            BLAST_ENGINEERING_2, BLAST_ENGINEERING_3, RAPID_BATTERY_1, RAPID_BATTERY_2, RAPID_BATTERY_3, TOXIC_BLOOM,
            CONCUSSIVE_BLAST, OVERPRESSURE);

    /**
     * Ticks between shots before any fire-rate buff.
     */
    private static final int COOLDOWN_MAX = 19;
    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private final TargetSelector targetSelector;
    private volatile float spreadRadius;
    private volatile float falloffSoftening = 1f;
    private volatile boolean concussiveBlast = false;
    private int coolDown = 0;
    private EnemyMob primaryTarget;
    private List<EnemyMob> splashTargets = List.of();
    private int splashCenterX;
    private int splashCenterY;

    public SplashTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SPLASH, PRICE, new TowerBaseStats(DAMAGE, RANGE, COOLDOWN_MAX), context, x, y);
        this.spreadRadius = SPREAD_RADIUS_BASE * context.getBoard().scale();
        this.targetSelector = new RandomSelector(context.random());
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
        if (node.equals(BLAST_ENGINEERING_1)) {
            this.spreadRadius *= BLAST_ENGINEERING_SPREAD_MULTIPLIER;
        } else if (node.equals(BLAST_ENGINEERING_2)) {
            this.falloffSoftening = FALLOFF_SOFTENING_FACTOR;
        } else if (node.equals(CONCUSSIVE_BLAST)) {
            this.concussiveBlast = true;
        }
    }

    private List<EnemyMob> findEnemiesInRangeVisible(int x, int y, float r) {
        return InRangeTargetQuery.ofType(x, y, r, EnemyMob.Type.NORMAL).matching(this.context.enemies());
    }

    private List<EnemyMob> findEnemiesInRange(int x, int y, float r) {
        return InRangeTargetQuery.anyType(x, y, r).matching(this.context.enemies());
    }

    public void doTick(int gameTime) {
        if (this.coolDown > 0) {
            this.coolDown--;
        } else {
            List<EnemyMob> enemies = this.findEnemiesInRangeVisible(this.centerX, this.centerY, this.rangeReal());
            Optional<EnemyMob> picked = this.targetSelector.selectFrom(enemies);

            if (picked.isPresent()) {
                this.primaryTarget = picked.get();
                int ex = (int) this.primaryTarget.getX();
                int ey = (int) this.primaryTarget.getY();
                int dx, dy, r2;
                int damage;

                this.splashTargets = this.findEnemiesInRange(ex, ey, this.spreadRadius);

                for (EnemyMob splashTarget : this.splashTargets) {
                    dx = ex - (int) splashTarget.getX();
                    dy = ey - (int) splashTarget.getY();
                    r2 = dx * dx + dy * dy;
                    damage = Math.round(this.damageCurrent()
                            * (1 - (r2 * this.falloffSoftening) / (this.spreadRadius * this.spreadRadius)));
                    this.dealDamage(splashTarget, Damage.physical(damage));
                    if (this.concussiveBlast) {
                        splashTarget.applyEffect(Effect.slow(SLOW_MULTIPLIER, SLOW_DURATION_TICKS,
                                d -> this.dealDamage(splashTarget, d)));
                    }
                }

                this.coolDown = this.coolDownCurrent();
                this.splashCenterX = ex;
                this.splashCenterY = ey;
            } else {
                this.primaryTarget = null;
            }
        }
        if (this.primaryTarget != null) {
            this.turretAim.tick(TurretAim.angleTo(this.centerX, this.centerY, this.primaryTarget.getX(), this.primaryTarget.getY()));
        }
    }

    public EnemyMob getPrimaryTarget() {
        return this.primaryTarget;
    }

    public TurretAim getTurretAim() {
        return this.turretAim;
    }

    public List<EnemyMob> getSplashTargets() {
        return this.splashTargets;
    }

    public float getSpreadRadius() {
        return this.spreadRadius;
    }

    public int getSplashCenterX() {
        return this.splashCenterX;
    }

    public int getSplashCenterY() {
        return this.splashCenterY;
    }

    public float getCoolDownFraction() {
        return (float) this.coolDown / this.coolDownCurrent();
    }

    public boolean isSplashVisible() {
        return this.coolDown >= this.coolDownCurrent();
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitSplashTower(this);
    }

    public String getInfoString() {
        return "Splash tower\n\n" +
                super.getInfoString() +
                "Splash radius " + SPREAD_RADIUS_BASE + "\n" +
                "Targets random";
    }

    public String getStatusString() {
        return "Splash tower\n\n" +
                super.getStatusString() +
                "Splash radius " + (this.spreadRadius / this.context.getBoard().scale()) + "\n" +
                "Targets random";
    }

}
