package td.tower;

import td.damage.Damage;
import td.enemy.EnemyMob;
import td.tower.buff.TowerBuff;
import td.tower.targeting.InRangeTargetQuery;
import td.tower.targeting.RandomSelector;
import td.tower.targeting.TargetSelector;
import td.tower.upgrade.ClusterCondition;
import td.tower.upgrade.DamageDealtCondition;
import td.tower.upgrade.UpgradePath;
import td.util.GameWorld;

import java.util.List;
import java.util.Optional;

/**
 * "Circle tower" - splash damage. Picks a random visible enemy in range, then damages
 * everything within {@code spreadRadius} of it, falling off with the square of the distance
 * from the blast centre. The splash deliberately uses an any-type query, so it is one of the
 * two towers that can hurt ghosts even though it cannot target them directly.
 */
public final class SplashTower extends AbstractTower {

    public static final int PRICE = 15;
    public static final int DAMAGE = 1600;
    public static final float RANGE = 3.2f;
    public static final float SPREAD_RADIUS_BASE = 1.75f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.25;

    /** How much bigger a splash "Siege" gives this tower's blast radius. */
    private static final float SIEGE_SPREAD_MULTIPLIER = 1.3f;
    /** More damage and a bigger blast - earned by this tower having proven itself already. */
    private static final UpgradePath SIEGE = new UpgradePath(
            "Siege", 35, new TowerBuff(0.35f, 0f, 0f, 0f), new DamageDealtCondition(20000));
    /** More damage and range - rewards a deliberately grouped placement rather than a solo one. */
    private static final UpgradePath CLUSTER_CHARGE = new UpgradePath(
            "Cluster Charge", 30, new TowerBuff(0.2f, 0.2f, 0f, 0f), new ClusterCondition(2));
    private static final List<UpgradePath> PATHS = List.of(SIEGE, CLUSTER_CHARGE);

    /** Ticks between shots before any fire-rate buff. */
    private static final int COOLDOWN_MAX = 19;

    // Bought on the EDT (onUpgradePathChosen) and read every tick on the game-loop thread, so
    // it is published volatile - CLAUDE.md 3 rule 2. Each is an independent scalar with no
    // invariant tying it to another, which is what makes a volatile scalar the right mechanism
    // here rather than a TowerStats-style snapshot: reading last pulse's value for one tick
    // after an upgrade is correct, just briefly stale.
    private volatile float spreadRadius;
    private int coolDown = 0;

    private EnemyMob primaryTarget;
    private List<EnemyMob> splashTargets = List.of();
    private int splashCenterX;
    private int splashCenterY;
    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);
    private final TargetSelector targetSelector;

    public SplashTower(GameWorld context, int x, int y) {
        super(TowerFactory.Type.SPLASH, PRICE, DAMAGE, RANGE, COOLDOWN_MAX, context, x, y);
        this.spreadRadius = SPREAD_RADIUS_BASE * context.getBoard().scale();
        this.targetSelector = new RandomSelector(context.random());
    }

    @Override
    public List<UpgradePath> availablePaths() {
        return PATHS;
    }

    /** Siege's blast-radius bump isn't a {@link TowerBuff} axis, so it's applied here instead. */
    @Override
    protected void onUpgradePathChosen(UpgradePath path) {
        if (path == SIEGE) {
            this.spreadRadius *= SIEGE_SPREAD_MULTIPLIER;
        }
    }

    private List<EnemyMob> findEnemiesInRangeVisible(int x, int y, float r) {
        return InRangeTargetQuery.ofType(x, y, r, EnemyMob.Type.Normal).matching(this.context.enemies());
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
                    damage = Math.round(this.damageCurrent() * (1 - r2 / (this.spreadRadius * this.spreadRadius)));
                    this.dealDamage(splashTarget, Damage.physical(damage));
                }

                this.coolDown = this.coolDownCurrent();
                this.splashCenterX = ex;
                this.splashCenterY = ey;
            } else {
                this.primaryTarget = null;
            }
        }
        // No target: hold the last heading rather than snapping back to a neutral angle - see
        // TurretAim's class doc on skipping tick() while idle.
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
        return "Circle tower\n\n" +
                super.getInfoString() +
                "Splash radius " + SPREAD_RADIUS_BASE + "\n" +
                "Targets random";
    }

    public String getStatusString() {
        return "Circle tower\n\n" +
                super.getStatusString() +
                "Splash radius " + (this.spreadRadius / this.context.getBoard().scale()) + "\n" +
                "Targets random";
    }

}
