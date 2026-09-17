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
import td.tower.upgrade.UpgradePath;
import td.util.GameWorld;

import java.util.List;

/**
 * "Mortar tower" - lobs a slow, unguided shell at whichever visible enemy is furthest along
 * the path, the same target choice as {@link TowerOne}. The shell travels in a straight line
 * to that enemy's position at the moment of firing and never re-aims, so a fast-moving enemy
 * can dodge it by the time it lands (see {@code CannonballProjectile}); on arrival it splashes
 * physical damage with the same distance-falloff shape {@link TowerTwo} uses, and slows every
 * enemy the blast reaches.
 */
public final class TowerMortar extends AbstractTower {

    public static final int price = 30;
    public static final int damage = 2000;
    public static final float range = 4.0f;
    public static final float splashRadiusBase = 2.0f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.3;
    private static final float PROJECTILE_SPEED = 40f;
    private static final float SLOW_MULTIPLIER_BASE = 0.5f;
    private static final int SLOW_DURATION_TICKS_BASE = 40;
    /** How much bigger a splash "Heavy Shell" gives this tower's blast radius - same shape as {@code TowerTwo}'s Siege. */
    private static final float HEAVY_SHELL_SPLASH_MULTIPLIER = 1.3f;
    private static final float CONCUSSIVE_CHARGE_SLOW_DURATION_MULTIPLIER = 1.5f;

    /** Bigger blast radius, earned by this tower having proven itself already. */
    private static final UpgradePath HEAVY_SHELL = new UpgradePath(
            "Heavy Shell", 35, new TowerBuff(0.4f, 0f, 0f, 0f), new DamageDealtCondition(20000));
    /** A longer-lasting slow, plus more range - rewards a deliberately grouped placement rather than a solo one. */
    private static final UpgradePath CONCUSSIVE_CHARGE = new UpgradePath(
            "Concussive Charge", 30, new TowerBuff(0f, 0.25f, 0f, 0f), new ClusterCondition(2));
    private static final List<UpgradePath> PATHS = List.of(HEAVY_SHELL, CONCUSSIVE_CHARGE);

    /** Ticks between shots before any fire-rate buff. */
    private static final int COOLDOWN_MAX = 50;

    private float splashRadius;
    private float slowMultiplier = SLOW_MULTIPLIER_BASE;
    private int slowDurationTicks = SLOW_DURATION_TICKS_BASE;
    private int coolDown = 0;
    private EnemyMob currentTarget;
    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);

    public TowerMortar(GameWorld context, int x, int y) {
        super(TowerFactory.type.mortar, price, damage, range, COOLDOWN_MAX, context, x, y);
        this.splashRadius = splashRadiusBase * context.getBoard().scale();
    }

    @Override
    public List<UpgradePath> availablePaths() {
        return PATHS;
    }

    /** Neither bonus is a {@link TowerBuff} axis, so each is applied here instead - same shape as {@code TowerTwo}'s Siege. */
    @Override
    protected void onUpgradePathChosen(UpgradePath path) {
        if (path == HEAVY_SHELL) {
            this.splashRadius *= HEAVY_SHELL_SPLASH_MULTIPLIER;
        } else if (path == CONCUSSIVE_CHARGE) {
            this.slowDurationTicks = Math.round(this.slowDurationTicks * CONCUSSIVE_CHARGE_SLOW_DURATION_MULTIPLIER);
        }
    }

    private EnemyMob findTarget() {
        List<EnemyMob> inRange = InRangeTargetQuery.ofType(this.centerX, this.centerY, this.rangeReal(), EnemyMob.type.Normal)
                .matching(this.context.getEnemyRegistry());
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
        this.context.addProjectile(new CannonballProjectile(this.centerX, this.centerY, target.getX(), target.getY(),
                PROJECTILE_SPEED, this::onImpact));
    }

    private void onImpact(double x, double y) {
        List<EnemyMob> hit = InRangeTargetQuery.anyType((int) Math.round(x), (int) Math.round(y), this.splashRadius)
                .matching(this.context.getEnemyRegistry());
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

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitTowerMortar(this);
    }

    public String getInfoString() {
        return "Mortar tower\n\n" +
                super.getInfoString() +
                "Lobs a slow, unguided shell\n" +
                "Splashes and slows on impact";
    }

    public String getStatusString() {
        return "Mortar tower\n\n" +
                super.getStatusString() +
                "Lobs a slow, unguided shell\n" +
                "Splashes and slows on impact";
    }
}
