package td.tower;

import td.enemy.EnemyMob;
import td.tower.targeting.InRangeTargetQuery;
import td.util.Context;

import java.util.List;

public final class TowerTwo extends AbstractTower {

    public static final int price = 15;
    public static final int damage = 1600;
    public static final float range = 3.2f;
    public static final float spreadRadiusBase = 1.75f;

    private final float spreadRadius;
    private int coolDown = 0;

    private EnemyMob primaryTarget;
    private List<EnemyMob> splashTargets = List.of();
    private int splashCenterX;
    private int splashCenterY;

    public TowerTwo(Context context, int x, int y) {
        super(TowerFactory.type.second, price, damage, range);
        this.name = "tower2";
        this.coolDownMax = 19;
        this.spreadRadius = spreadRadiusBase * context.scale;
        this.doInit(context, x, y);
    }

    private List<EnemyMob> findEnemiesInRangeVisible(int x, int y, float r) {
        return InRangeTargetQuery.ofType(x, y, r, EnemyMob.type.Normal).matching(this.context);
    }

    private List<EnemyMob> findEnemiesInRange(int x, int y, float r) {
        return InRangeTargetQuery.anyType(x, y, r).matching(this.context);
    }

    public void doTick(int gameTime) {
        if (this.coolDown > 0) {
            this.coolDown--;
        } else {
            List<EnemyMob> enemies = this.findEnemiesInRangeVisible(this.centerX, this.centerY, this.rangeReal);

            if (!enemies.isEmpty()) {
                this.primaryTarget = enemies.get((int) (Math.random() * enemies.size()));
                int ex = this.primaryTarget.getX();
                int ey = this.primaryTarget.getY();
                int dx, dy, r2;
                int damage;

                this.splashTargets = this.findEnemiesInRange(ex, ey, this.spreadRadius);

                for (EnemyMob splashTarget : this.splashTargets) {
                    dx = ex - splashTarget.getX();
                    dy = ey - splashTarget.getY();
                    r2 = dx * dx + dy * dy;
                    damage = Math.round(this.damageCurrent * (1 - r2 / (this.spreadRadius * this.spreadRadius)));
                    splashTarget.doDamage(damage);
                }

                this.coolDown = this.coolDownMax;
                this.splashCenterX = ex;
                this.splashCenterY = ey;
            } else {
                this.primaryTarget = null;
            }
        }
    }

    public EnemyMob getPrimaryTarget() {
        return this.primaryTarget;
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
