package td.tower;

import td.damage.Damage;
import td.enemy.EnemyMob;
import td.tower.targeting.InRangeTargetQuery;
import td.util.GameWorld;

import java.util.List;

public final class TowerTwo extends AbstractTower {

    public static final int price = 15;
    public static final int damage = 1600;
    public static final float range = 3.2f;
    public static final float spreadRadiusBase = 1.75f;

    private static final double MAX_TURN_RADIANS_PER_TICK = 0.25;

    private final float spreadRadius;
    private int coolDown = 0;

    private EnemyMob primaryTarget;
    private List<EnemyMob> splashTargets = List.of();
    private int splashCenterX;
    private int splashCenterY;
    private final TurretAim turretAim = new TurretAim(MAX_TURN_RADIANS_PER_TICK);

    public TowerTwo(GameWorld context, int x, int y) {
        super(TowerFactory.type.second, price, damage, range);
        this.coolDownMax = 19;
        this.spreadRadius = spreadRadiusBase * context.getBoard().scale();
        this.doInit(context, x, y);
    }

    private List<EnemyMob> findEnemiesInRangeVisible(int x, int y, float r) {
        return InRangeTargetQuery.ofType(x, y, r, EnemyMob.type.Normal).matching(this.context.getEnemyRegistry());
    }

    private List<EnemyMob> findEnemiesInRange(int x, int y, float r) {
        return InRangeTargetQuery.anyType(x, y, r).matching(this.context.getEnemyRegistry());
    }

    public void doTick(int gameTime) {
        if (this.coolDown > 0) {
            this.coolDown--;
        } else {
            List<EnemyMob> enemies = this.findEnemiesInRangeVisible(this.centerX, this.centerY, this.rangeReal);

            if (!enemies.isEmpty()) {
                this.primaryTarget = enemies.get((int) (Math.random() * enemies.size()));
                int ex = (int) this.primaryTarget.getX();
                int ey = (int) this.primaryTarget.getY();
                int dx, dy, r2;
                int damage;

                this.splashTargets = this.findEnemiesInRange(ex, ey, this.spreadRadius);

                for (EnemyMob splashTarget : this.splashTargets) {
                    dx = ex - (int) splashTarget.getX();
                    dy = ey - (int) splashTarget.getY();
                    r2 = dx * dx + dy * dy;
                    damage = Math.round(this.damageCurrent * (1 - r2 / (this.spreadRadius * this.spreadRadius)));
                    this.dealDamage(splashTarget, Damage.of(damage));
                }

                this.coolDown = this.coolDownMax;
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
        return (float) this.coolDown / this.coolDownMax;
    }

    public boolean isSplashVisible() {
        return this.coolDown >= this.coolDownMax;
    }

    public <R> R accept(TowerVisitor<R> visitor) {
        return visitor.visitTowerTwo(this);
    }

    public String getInfoString() {
        return "Circle tower\n\n" +
                super.getInfoString() +
                "Splash radius " + spreadRadiusBase + "\n" +
                "Targets random";
    }

    public String getStatusString() {
        return "Circle tower\n\n" +
                super.getStatusString() +
                "Splash radius " + spreadRadiusBase + "\n" +
                "Targets random";
    }

}
